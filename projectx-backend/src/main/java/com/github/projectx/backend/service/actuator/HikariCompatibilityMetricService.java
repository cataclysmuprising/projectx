package com.github.projectx.backend.service.actuator;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

/**
 * Publishes a compatibility metric named `hikaricp` so
 * `/actuator/metrics/hikaricp` is queryable without 404.
 * <p>
 * Use the `component` tag values:
 * active, idle, pending, total, max, min, utilization_percent
 */
@Service
public class HikariCompatibilityMetricService {

	private final HikariDataSource hikariDataSource;

	public HikariCompatibilityMetricService(
			ObjectProvider<MeterRegistry> meterRegistryProvider,
			ObjectProvider<DataSource> dataSourceProvider
	) {
		MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
		DataSource dataSource = dataSourceProvider.getIfAvailable();

		if (meterRegistry == null || !(dataSource instanceof HikariDataSource hikari)) {
			hikariDataSource = null;
			return;
		}

		hikariDataSource = hikari;
		String poolName = StringUtils.defaultIfBlank(hikari.getPoolName(), "default");

		registerAllGauges(meterRegistry, "hikaricp", poolName);
		// Keep compatibility for common typo: /actuator/metrics/hikaaricp
		registerAllGauges(meterRegistry, "hikaaricp", poolName);
	}

	private void registerAllGauges(MeterRegistry meterRegistry, String metricName, String poolName) {
		registerGauge(meterRegistry, metricName, poolName, "active");
		registerGauge(meterRegistry, metricName, poolName, "idle");
		registerGauge(meterRegistry, metricName, poolName, "pending");
		registerGauge(meterRegistry, metricName, poolName, "total");
		registerGauge(meterRegistry, metricName, poolName, "max");
		registerGauge(meterRegistry, metricName, poolName, "min");
		registerGauge(meterRegistry, metricName, poolName, "utilization_percent");
	}

	private void registerGauge(MeterRegistry meterRegistry, String metricName, String poolName, String component) {
		Gauge.builder(metricName, this, service -> service.readComponentValue(component))
				.description("HikariCP compatibility gauge. Filter by tag component.")
				.tag("pool", poolName)
				.tag("component", component)
				.strongReference(true)
				.register(meterRegistry);
	}

	private double readComponentValue(String component) {
		if (hikariDataSource == null) {
			return 0d;
		}

		HikariPoolMXBean poolMxBean = hikariDataSource.getHikariPoolMXBean();
		int active = poolMxBean == null ? 0 : poolMxBean.getActiveConnections();
		int idle = poolMxBean == null ? 0 : poolMxBean.getIdleConnections();
		int pending = poolMxBean == null ? 0 : poolMxBean.getThreadsAwaitingConnection();
		int total = poolMxBean == null ? 0 : poolMxBean.getTotalConnections();
		int max = hikariDataSource.getMaximumPoolSize();
		int min = hikariDataSource.getMinimumIdle();

		return switch (component) {
			case "active" -> active;
			case "idle" -> idle;
			case "pending" -> pending;
			case "total" -> total;
			case "max" -> max;
			case "min" -> min;
			case "utilization_percent" -> max > 0 ? (active * 100.0d) / max : 0d;
			default -> 0d;
		};
	}
}
