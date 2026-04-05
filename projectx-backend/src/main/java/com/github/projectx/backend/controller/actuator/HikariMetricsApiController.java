package com.github.projectx.backend.controller.actuator;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.time.ZonedDateTime;
import java.util.*;

/**
 * Aggregated HikariCP metrics view under `/actuator/metrics/hikaricp/summary`.
 */
@RestController
public class HikariMetricsApiController {

	private final MeterRegistry meterRegistry;
	private final HikariDataSource hikariDataSource;

	public HikariMetricsApiController(
			ObjectProvider<MeterRegistry> meterRegistryProvider,
			ObjectProvider<DataSource> dataSourceProvider
	) {
		meterRegistry = meterRegistryProvider.getIfAvailable();
		DataSource dataSource = dataSourceProvider.getIfAvailable();
		hikariDataSource = dataSource instanceof HikariDataSource hikari ? hikari : null;
	}

	@GetMapping(
			value = {"/actuator/metrics/hikaricp/summary", "/actuator/metrics/hikaaricp/summary"},
			produces = MediaType.APPLICATION_JSON_VALUE
	)
	public ResponseEntity<Map<String, Object>> summary() {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("name", "hikaricp.summary");
		response.put("timestamp", ZonedDateTime.now().toString());

		if (meterRegistry == null) {
			response.put("status", "ERROR");
			response.put("message", "MeterRegistry is not available.");
			response.put("pools", List.of());
			return ResponseEntity.ok(response);
		}

		Set<String> pools = resolvePoolNames();
		if (pools.isEmpty()) {
			response.put("status", "WARN");
			response.put("message", "No Hikari pool metrics have been published yet.");
			response.put("pools", List.of());
			response.put("totals", Map.of());
			return ResponseEntity.ok(response);
		}

		Map<String, Object> totals = new LinkedHashMap<>();
		double totalActive = 0d;
		double totalIdle = 0d;
		double totalPending = 0d;
		double totalMax = 0d;
		double totalMin = 0d;
		double totalConnections = 0d;
		List<Map<String, Object>> poolMetrics = new ArrayList<>();
		List<String> recommendations = new ArrayList<>();

		for (String pool : pools) {
			double active = coalesce(readGauge("hikaricp.connections.active", pool));
			double idle = coalesce(readGauge("hikaricp.connections.idle", pool));
			double pending = coalesce(readGauge("hikaricp.connections.pending", pool));
			double max = coalesce(readGauge("hikaricp.connections.max", pool));
			double min = coalesce(readGauge("hikaricp.connections.min", pool));
			double connections = coalesce(readGauge("hikaricp.connections", pool));
			double utilizationPercent = max > 0d ? (active / max) * 100d : 0d;
			double headroom = Math.max(0d, max - active);
			String poolState = evaluatePoolState(utilizationPercent, pending);

			totalActive += active;
			totalIdle += idle;
			totalPending += pending;
			totalMax += max;
			totalMin += min;
			totalConnections += connections;

			Map<String, Object> row = new LinkedHashMap<>();
			row.put("pool", pool);
			row.put("activeConnections", active);
			row.put("idleConnections", idle);
			row.put("pendingConnections", pending);
			row.put("totalConnections", connections);
			row.put("maxConnections", max);
			row.put("minConnections", min);
			row.put("headroomConnections", headroom);
			row.put("utilizationPercent", utilizationPercent);
			row.put("state", poolState);
			poolMetrics.add(row);

			if ("CRITICAL".equals(poolState)) {
				recommendations.add("Pool " + pool + " is saturated. Check query latency and increase max pool size if needed.");
			}
			else if ("WARN".equals(poolState)) {
				recommendations.add("Pool " + pool + " is under pressure. Monitor pending connections and slow queries.");
			}
		}

		totals.put("activeConnections", totalActive);
		totals.put("idleConnections", totalIdle);
		totals.put("pendingConnections", totalPending);
		totals.put("totalConnections", totalConnections);
		totals.put("maxConnections", totalMax);
		totals.put("minConnections", totalMin);
		totals.put("utilizationPercent", totalMax > 0d ? (totalActive / totalMax) * 100d : 0d);
		totals.put("state", evaluatePoolState(totalMax > 0d ? (totalActive / totalMax) * 100d : 0d, totalPending));

		response.put("pools", poolMetrics);
		response.put("totals", totals);
		response.put("status", "OK");
		response.put("recommendations", recommendations);
		response.put("datasource", buildDatasourceConfig());
		response.put("sourceMetrics", List.of(
				"hikaricp.connections",
				"hikaricp.connections.active",
				"hikaricp.connections.idle",
				"hikaricp.connections.pending",
				"hikaricp.connections.max",
				"hikaricp.connections.min"
		));

		return ResponseEntity.ok(response);
	}

	private Set<String> resolvePoolNames() {
		Set<String> poolNames = new TreeSet<>();
		for (Meter meter : meterRegistry.getMeters()) {
			String meterName = meter.getId().getName();
			if (!meterName.startsWith("hikaricp.connections")) {
				continue;
			}
			meter.getId().getTags().forEach(tag -> {
				if ("pool".equals(tag.getKey())) {
					tag.getValue();
					if (!tag.getValue().isBlank()) {
						poolNames.add(tag.getValue());
					}
				}
			});
		}

		if (hikariDataSource != null && hikariDataSource.getPoolName() != null && !hikariDataSource.getPoolName().isBlank()) {
			poolNames.add(hikariDataSource.getPoolName());
		}

		return poolNames;
	}

	private Double readGauge(String metricName, String poolName) {
		if (meterRegistry == null || metricName == null || metricName.isBlank()) {
			return null;
		}

		Collection<Gauge> gauges = poolName == null || poolName.isBlank()
				? meterRegistry.find(metricName).gauges()
				: meterRegistry.find(metricName).tag("pool", poolName).gauges();

		if (!gauges.isEmpty()) {
			return gauges.stream().mapToDouble(Gauge::value).sum();
		}

		Gauge gauge = meterRegistry.find(metricName).gauge();
		return gauge == null ? null : gauge.value();
	}

	private double coalesce(Double value) {
		return value == null ? 0d : value;
	}

	private String evaluatePoolState(double utilizationPercent, double pendingConnections) {
		if (utilizationPercent >= 95d || pendingConnections >= 10d) {
			return "CRITICAL";
		}
		if (utilizationPercent >= 80d || pendingConnections > 0d) {
			return "WARN";
		}
		return "HEALTHY";
	}

	private Map<String, Object> buildDatasourceConfig() {
		if (hikariDataSource == null) {
			return Map.of("type", "unknown");
		}

		Map<String, Object> config = new LinkedHashMap<>();
		config.put("type", hikariDataSource.getClass().getSimpleName());
		config.put("poolName", hikariDataSource.getPoolName());
		config.put("jdbcUrl", sanitizeJdbcUrl(hikariDataSource.getJdbcUrl()));
		config.put("maxPoolSize", hikariDataSource.getMaximumPoolSize());
		config.put("minIdle", hikariDataSource.getMinimumIdle());
		config.put("connectionTimeoutMs", hikariDataSource.getConnectionTimeout());
		config.put("validationTimeoutMs", hikariDataSource.getValidationTimeout());
		config.put("maxLifetimeMs", hikariDataSource.getMaxLifetime());
		config.put("idleTimeoutMs", hikariDataSource.getIdleTimeout());
		return config;
	}

	private String sanitizeJdbcUrl(String jdbcUrl) {
		if (jdbcUrl == null || jdbcUrl.isBlank()) {
			return "unknown";
		}
		int paramsIndex = jdbcUrl.indexOf('?');
		return paramsIndex < 0 ? jdbcUrl : jdbcUrl.substring(0, paramsIndex);
	}
}
