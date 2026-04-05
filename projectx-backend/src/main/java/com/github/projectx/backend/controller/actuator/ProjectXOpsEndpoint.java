package com.github.projectx.backend.controller.actuator;

import com.github.projectx.backend.service.actuator.RedisCommandObservabilityService;
import io.micrometer.core.instrument.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Aggregated operational summary endpoint for project-specific observability.
 */
@Component
@Endpoint(id = "projectx")
public class ProjectXOpsEndpoint {

	private static final String STATE_UNKNOWN = "UNKNOWN";
	private static final String STATE_IDLE = "IDLE";
	private static final String STATE_HEALTHY = "HEALTHY";
	private static final String STATE_WARN = "WARN";
	private static final String STATE_CRITICAL = "CRITICAL";

	private final MeterRegistry meterRegistry;
	private final ApplicationAvailability applicationAvailability;
	private final RedisCommandObservabilityService redisCommandObservabilityService;
	private final StringRedisTemplate redisTemplate;
	private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

	private final double apiLatencyWarnMs;
	private final double apiLatencyCriticalMs;
	private final double apiServerErrorWarnPercent;
	private final double apiServerErrorCriticalPercent;
	private final double walletFailureWarnPercent;
	private final double walletFailureCriticalPercent;
	private final double dbUtilizationWarnPercent;
	private final double dbUtilizationCriticalPercent;
	private final double dbPendingWarn;
	private final double dbPendingCritical;
	private final double redisLatencyWarnMs;
	private final double redisLatencyCriticalMs;
	private final double redisFailureWarnPercent;
	private final double redisFailureCriticalPercent;
	private final double redisConsecutiveFailureCritical;

	public ProjectXOpsEndpoint(
			ObjectProvider<MeterRegistry> meterRegistryProvider,
			ObjectProvider<ApplicationAvailability> applicationAvailabilityProvider,
			ObjectProvider<RedisCommandObservabilityService> redisCommandObservabilityServiceProvider,
			ObjectProvider<StringRedisTemplate> redisTemplateProvider,
			@Value("${app.actuator.thresholds.api-latency-warn-ms:400}") double apiLatencyWarnMs,
			@Value("${app.actuator.thresholds.api-latency-critical-ms:1000}") double apiLatencyCriticalMs,
			@Value("${app.actuator.thresholds.api-server-error-warn-percent:1.0}") double apiServerErrorWarnPercent,
			@Value("${app.actuator.thresholds.api-server-error-critical-percent:3.0}") double apiServerErrorCriticalPercent,
			@Value("${app.actuator.thresholds.wallet-failure-warn-percent:2.0}") double walletFailureWarnPercent,
			@Value("${app.actuator.thresholds.wallet-failure-critical-percent:5.0}") double walletFailureCriticalPercent,
			@Value("${app.actuator.thresholds.db-utilization-warn-percent:80}") double dbUtilizationWarnPercent,
			@Value("${app.actuator.thresholds.db-utilization-critical-percent:95}") double dbUtilizationCriticalPercent,
			@Value("${app.actuator.thresholds.db-pending-warn:1}") double dbPendingWarn,
			@Value("${app.actuator.thresholds.db-pending-critical:10}") double dbPendingCritical,
			@Value("${app.actuator.thresholds.redis-latency-warn-ms:25}") double redisLatencyWarnMs,
			@Value("${app.actuator.thresholds.redis-latency-critical-ms:100}") double redisLatencyCriticalMs,
			@Value("${app.actuator.thresholds.redis-failure-warn-percent:1.0}") double redisFailureWarnPercent,
			@Value("${app.actuator.thresholds.redis-failure-critical-percent:3.0}") double redisFailureCriticalPercent,
			@Value("${app.actuator.thresholds.redis-consecutive-failure-critical:3}") double redisConsecutiveFailureCritical
	) {
		meterRegistry = meterRegistryProvider.getIfAvailable();
		applicationAvailability = applicationAvailabilityProvider.getIfAvailable();
		redisCommandObservabilityService = redisCommandObservabilityServiceProvider.getIfAvailable();
		redisTemplate = redisTemplateProvider.getIfAvailable();
		this.apiLatencyWarnMs = apiLatencyWarnMs;
		this.apiLatencyCriticalMs = apiLatencyCriticalMs;
		this.apiServerErrorWarnPercent = apiServerErrorWarnPercent;
		this.apiServerErrorCriticalPercent = apiServerErrorCriticalPercent;
		this.walletFailureWarnPercent = walletFailureWarnPercent;
		this.walletFailureCriticalPercent = walletFailureCriticalPercent;
		this.dbUtilizationWarnPercent = dbUtilizationWarnPercent;
		this.dbUtilizationCriticalPercent = dbUtilizationCriticalPercent;
		this.dbPendingWarn = dbPendingWarn;
		this.dbPendingCritical = dbPendingCritical;
		this.redisLatencyWarnMs = redisLatencyWarnMs;
		this.redisLatencyCriticalMs = redisLatencyCriticalMs;
		this.redisFailureWarnPercent = redisFailureWarnPercent;
		this.redisFailureCriticalPercent = redisFailureCriticalPercent;
		this.redisConsecutiveFailureCritical = redisConsecutiveFailureCritical;
	}

	@ReadOperation
	public Map<String, Object> summary() {
		Map<String, Object> availability = availabilitySection();
		Map<String, Object> jvm = jvmSection();
		Map<String, Object> wallet = walletSection();
		Map<String, Object> api = apiSection();
		Map<String, Object> database = databaseSection();
		Map<String, Object> redis = redisSection();

		Map<String, Object> root = new LinkedHashMap<>();
		root.put("timestamp", ZonedDateTime.now().toString());
		root.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
		root.put("availability", availability);
		root.put("jvm", jvm);
		root.put("wallet", wallet);
		root.put("api", api);
		root.put("database", database);
		root.put("redis", redis);
		root.put("overallState", resolveOverallState(wallet, api, database, redis, availability));
		root.put("thresholds", thresholdsSection());
		return root;
	}

	private Map<String, Object> availabilitySection() {
		Map<String, Object> availability = new LinkedHashMap<>();
		if (applicationAvailability == null) {
			availability.put("liveness", STATE_UNKNOWN);
			availability.put("readiness", STATE_UNKNOWN);
			return availability;
		}

		availability.put("liveness", applicationAvailability.getLivenessState().name());
		availability.put("readiness", applicationAvailability.getReadinessState().name());
		return availability;
	}

	private Map<String, Object> jvmSection() {
		Map<String, Object> jvm = new LinkedHashMap<>();
		long heapUsedBytes = memoryMXBean.getHeapMemoryUsage().getUsed();
		long heapMaxBytes = memoryMXBean.getHeapMemoryUsage().getMax();
		jvm.put("availableProcessors", Runtime.getRuntime().availableProcessors());
		jvm.put("heapUsedBytes", heapUsedBytes);
		jvm.put("heapMaxBytes", heapMaxBytes);
		jvm.put("heapUsagePercent", heapMaxBytes > 0 ? ((double) heapUsedBytes / (double) heapMaxBytes) * 100d : null);
		jvm.put("nonHeapUsedBytes", memoryMXBean.getNonHeapMemoryUsage().getUsed());
		jvm.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
		jvm.put("peakThreadCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
		return jvm;
	}

	private Map<String, Object> walletSection() {
		Map<String, Object> wallet = new LinkedHashMap<>();
		Double tpmSuccess = readGauge("projectx.wallet.transactions.per_minute", "status", "success");
		Double tpmFailed = readGauge("projectx.wallet.transactions.per_minute", "status", "failed");
		Double totalAttempts = readMeterSum("projectx.wallet.transactions.total", Statistic.COUNT);

		double success = coalesce(tpmSuccess);
		double failed = coalesce(tpmFailed);
		double total = success + failed;
		Double failedPercent = total > 0d ? (failed / total) * 100d : null;

		wallet.put("transactionsPerMinuteSuccess", success);
		wallet.put("transactionsPerMinuteFailed", failed);
		wallet.put("transactionsPerMinuteTotal", total);
		wallet.put("transactionsTotal", totalAttempts);
		wallet.put("failureRatePercent", failedPercent);
		wallet.put("state", walletState(total, failedPercent));
		return wallet;
	}

	private Map<String, Object> apiSection() {
		Map<String, Object> api = new LinkedHashMap<>();
		Double httpCount = readMeterSum("http.server.requests", Statistic.COUNT);
		Double httpTotal = readMeterSum("http.server.requests", Statistic.TOTAL_TIME);
		Double httpMax = readMeterSum("http.server.requests", Statistic.MAX);
		Double httpSuccessCount = readMeterSum("http.server.requests", Statistic.COUNT, "outcome", "SUCCESS");
		Double httpRedirectionCount = readMeterSum("http.server.requests", Statistic.COUNT, "outcome", "REDIRECTION");
		Double httpServerErrorCount = readMeterSum("http.server.requests", Statistic.COUNT, "outcome", "SERVER_ERROR");
		Double httpClientErrorCount = readMeterSum("http.server.requests", Statistic.COUNT, "outcome", "CLIENT_ERROR");

		Double averageLatencyMs = null;
		Double successRatePercent = null;
		Double redirectionRatePercent = null;
		Double serverErrorRatePercent = null;
		Double clientErrorRatePercent = null;

		if (httpCount != null && httpTotal != null && httpCount > 0d) {
			averageLatencyMs = (httpTotal / httpCount) * 1000d;
			successRatePercent = percentage(httpSuccessCount, httpCount);
			redirectionRatePercent = percentage(httpRedirectionCount, httpCount);
			serverErrorRatePercent = percentage(httpServerErrorCount, httpCount);
			clientErrorRatePercent = percentage(httpClientErrorCount, httpCount);
		}

		api.put("httpRequestCount", httpCount);
		api.put("httpTotalTimeSeconds", httpTotal);
		api.put("httpMaxLatencyMs", httpMax == null ? null : httpMax * 1000d);
		api.put("httpSuccessCount", httpSuccessCount);
		api.put("httpRedirectionCount", httpRedirectionCount);
		api.put("httpServerErrorCount", httpServerErrorCount);
		api.put("httpClientErrorCount", httpClientErrorCount);
		api.put("httpAverageLatencyMs", averageLatencyMs);
		api.put("httpSuccessRatePercent", successRatePercent);
		api.put("httpRedirectionRatePercent", redirectionRatePercent);
		api.put("httpServerErrorRatePercent", serverErrorRatePercent);
		api.put("httpClientErrorRatePercent", clientErrorRatePercent);
		api.put("state", apiState(httpCount, averageLatencyMs, serverErrorRatePercent));
		return api;
	}

	private Map<String, Object> databaseSection() {
		Map<String, Object> db = new LinkedHashMap<>();
		Double activeConnections = readGauge("hikaricp.connections.active");
		Double idleConnections = readGauge("hikaricp.connections.idle");
		Double pendingConnections = readGauge("hikaricp.connections.pending");
		Double maxConnections = readGauge("hikaricp.connections.max");
		Double acquireCount = readMeterSum("hikaricp.connections.acquire", Statistic.COUNT);
		Double acquireTotalTimeSeconds = readMeterSum("hikaricp.connections.acquire", Statistic.TOTAL_TIME);
		Double timeoutCount = readMeterSum("hikaricp.connections.timeout", Statistic.COUNT);
		boolean hasPoolMetrics = activeConnections != null
				|| idleConnections != null
				|| pendingConnections != null
				|| maxConnections != null;

		double active = coalesce(activeConnections);
		double max = coalesce(maxConnections);
		double pending = coalesce(pendingConnections);
		Double utilizationPercent = max > 0d ? Double.valueOf((active / max) * 100d) : (hasPoolMetrics ? 0d : null);
		Double acquireAverageLatencyMs = null;
		if (acquireCount != null && acquireTotalTimeSeconds != null && acquireCount > 0d) {
			acquireAverageLatencyMs = (acquireTotalTimeSeconds / acquireCount) * 1000d;
		}

		db.put("activeConnections", activeConnections);
		db.put("idleConnections", idleConnections);
		db.put("pendingConnections", pendingConnections);
		db.put("maxConnections", maxConnections);
		db.put("utilizationPercent", utilizationPercent);
		db.put("acquireCount", acquireCount);
		db.put("acquireTimeoutCount", timeoutCount);
		db.put("acquireAverageLatencyMs", acquireAverageLatencyMs);
		db.put("state", hasPoolMetrics ? databaseState(coalesce(utilizationPercent), pending) : STATE_IDLE);
		return db;
	}

	private Map<String, Object> redisSection() {
		Map<String, Object> redis = new LinkedHashMap<>();
		if (redisCommandObservabilityService != null) {
			redisCommandObservabilityService.probeRedisPing();
		}

		Double commandCount = readMeterSum("redis.command", Statistic.COUNT);
		Double commandTotalTimeSeconds = readMeterSum("redis.command", Statistic.TOTAL_TIME);
		Double commandFailureCount = readMeterSum("redis.command", Statistic.COUNT, "result", "failure");
		Double lastLatencyMs = readGauge("redis.command.last.latency.ms");
		Double probeErrors = readMeterSum("redis.command.errors", Statistic.COUNT);
		Double lastSuccessEpochMs = readGauge("redis.command.last.success.epoch.ms");
		Double lastFailureEpochMs = readGauge("redis.command.last.failure.epoch.ms");
		Double consecutiveFailures = readGauge("redis.command.consecutive.failures");

		boolean hasRedisCommandMetrics = commandCount != null && commandCount > 0d;
		boolean redisProbeSuccess = false;
		Double redisProbeLatencyMs = null;
		String redisProbeError = null;
		if (!hasRedisCommandMetrics && redisTemplate != null) {
			long startedNs = System.nanoTime();
			try {
				String pong = redisTemplate.execute(RedisConnectionCommands::ping);
				redisProbeSuccess = StringUtils.isNotBlank(pong);
			}
			catch (Exception e) {
				redisProbeError = e.getMessage();
			}
			finally {
				redisProbeLatencyMs = (System.nanoTime() - startedNs) / 1_000_000d;
			}

			commandCount = 1d;
			commandTotalTimeSeconds = redisProbeSuccess
					? redisProbeLatencyMs / 1000d
					: 0d;
			commandFailureCount = redisProbeSuccess ? 0d : 1d;
			lastLatencyMs = redisProbeLatencyMs;
			consecutiveFailures = redisProbeSuccess ? 0d : 1d;
		}

		Double averageLatencyMs = null;
		Double failureRatePercent = null;
		if (commandCount != null && commandCount > 0d) {
			averageLatencyMs = commandTotalTimeSeconds == null ? null : (commandTotalTimeSeconds / commandCount) * 1000d;
			failureRatePercent = percentage(commandFailureCount, commandCount);
		}

		redis.put("commandCount", commandCount);
		redis.put("commandTotalTimeSeconds", commandTotalTimeSeconds);
		redis.put("commandFailures", commandFailureCount);
		redis.put("lastLatencyMs", lastLatencyMs);
		redis.put("probeErrors", probeErrors);
		redis.put("lastSuccessEpochMs", lastSuccessEpochMs);
		redis.put("lastFailureEpochMs", lastFailureEpochMs);
		redis.put("consecutiveFailures", consecutiveFailures);
		redis.put("averageLatencyMs", averageLatencyMs);
		redis.put("failureRatePercent", failureRatePercent);
		if (!hasRedisCommandMetrics) {
			redis.put("probeMode", redisTemplate == null ? "UNAVAILABLE" : "MANUAL");
		}
		if (redisProbeLatencyMs != null) {
			redis.put("probeLatencyMs", redisProbeLatencyMs);
		}
		if (redisProbeError != null) {
			redis.put("probeError", redisProbeError);
		}
		redis.put("state", redisState(commandCount, lastLatencyMs, failureRatePercent, consecutiveFailures));
		if (lastSuccessEpochMs != null && lastSuccessEpochMs > 0d) {
			redis.put("lastSuccessAgeSeconds", Math.max(0d, (System.currentTimeMillis() - lastSuccessEpochMs) / 1000d));
		}
		return redis;
	}

	private Map<String, Object> thresholdsSection() {
		Map<String, Object> thresholds = new LinkedHashMap<>();
		Map<String, Object> api = new LinkedHashMap<>();
		api.put("latencyWarnMs", apiLatencyWarnMs);
		api.put("latencyCriticalMs", apiLatencyCriticalMs);
		api.put("serverErrorWarnPercent", apiServerErrorWarnPercent);
		api.put("serverErrorCriticalPercent", apiServerErrorCriticalPercent);
		thresholds.put("api", api);

		Map<String, Object> wallet = new LinkedHashMap<>();
		wallet.put("failureWarnPercent", walletFailureWarnPercent);
		wallet.put("failureCriticalPercent", walletFailureCriticalPercent);
		thresholds.put("wallet", wallet);

		Map<String, Object> db = new LinkedHashMap<>();
		db.put("utilizationWarnPercent", dbUtilizationWarnPercent);
		db.put("utilizationCriticalPercent", dbUtilizationCriticalPercent);
		db.put("pendingWarn", dbPendingWarn);
		db.put("pendingCritical", dbPendingCritical);
		thresholds.put("database", db);

		Map<String, Object> redis = new LinkedHashMap<>();
		redis.put("latencyWarnMs", redisLatencyWarnMs);
		redis.put("latencyCriticalMs", redisLatencyCriticalMs);
		redis.put("failureWarnPercent", redisFailureWarnPercent);
		redis.put("failureCriticalPercent", redisFailureCriticalPercent);
		redis.put("consecutiveFailureCritical", redisConsecutiveFailureCritical);
		thresholds.put("redis", redis);
		return thresholds;
	}

	private String walletState(double total, Double failedPercent) {
		if (total <= 0d) {
			return STATE_IDLE;
		}
		if (failedPercent == null) {
			return STATE_UNKNOWN;
		}
		return severity(failedPercent, walletFailureWarnPercent, walletFailureCriticalPercent);
	}

	private String apiState(Double requestCount, Double averageLatencyMs, Double serverErrorRatePercent) {
		if (requestCount == null || requestCount <= 0d) {
			return STATE_IDLE;
		}
		String latencyState = averageLatencyMs == null
				? STATE_UNKNOWN
				: severity(averageLatencyMs, apiLatencyWarnMs, apiLatencyCriticalMs);
		String errorState = serverErrorRatePercent == null
				? STATE_UNKNOWN
				: severity(serverErrorRatePercent, apiServerErrorWarnPercent, apiServerErrorCriticalPercent);
		return higherSeverity(latencyState, errorState);
	}

	private String databaseState(double utilizationPercent, double pending) {
		String utilizationState = severity(utilizationPercent, dbUtilizationWarnPercent, dbUtilizationCriticalPercent);
		String pendingState = severity(pending, dbPendingWarn, dbPendingCritical);
		return higherSeverity(utilizationState, pendingState);
	}

	private String redisState(
			Double commandCount,
			Double lastLatencyMs,
			Double failureRatePercent,
			Double consecutiveFailures
	) {
		if (commandCount == null || commandCount <= 0d) {
			return STATE_IDLE;
		}

		String latencyState = lastLatencyMs == null
				? STATE_UNKNOWN
				: severity(lastLatencyMs, redisLatencyWarnMs, redisLatencyCriticalMs);
		String failureState = failureRatePercent == null
				? STATE_UNKNOWN
				: severity(failureRatePercent, redisFailureWarnPercent, redisFailureCriticalPercent);
		String consecutiveState = consecutiveFailures != null && consecutiveFailures >= redisConsecutiveFailureCritical
				? STATE_CRITICAL
				: STATE_HEALTHY;

		return higherSeverity(higherSeverity(latencyState, failureState), consecutiveState);
	}

	private String severity(double value, double warnThreshold, double criticalThreshold) {
		if (value >= criticalThreshold) {
			return STATE_CRITICAL;
		}
		if (value >= warnThreshold) {
			return STATE_WARN;
		}
		return STATE_HEALTHY;
	}

	private String higherSeverity(String left, String right) {
		if (rankSeverity(left) >= rankSeverity(right)) {
			return left;
		}
		return right;
	}

	private int rankSeverity(String value) {
		if (STATE_CRITICAL.equals(value)) {
			return 4;
		}
		if (STATE_WARN.equals(value)) {
			return 3;
		}
		if (STATE_UNKNOWN.equals(value)) {
			return 2;
		}
		if (STATE_IDLE.equals(value)) {
			return 1;
		}
		return 0;
	}

	private String resolveOverallState(
			Map<String, Object> wallet,
			Map<String, Object> api,
			Map<String, Object> database,
			Map<String, Object> redis,
			Map<String, Object> availability
	) {
		String overall = STATE_HEALTHY;
		overall = higherSeverity(overall, asState(wallet.get("state")));
		overall = higherSeverity(overall, asState(api.get("state")));
		overall = higherSeverity(overall, asState(database.get("state")));
		overall = higherSeverity(overall, asState(redis.get("state")));

		String liveness = String.valueOf(availability.getOrDefault("liveness", STATE_UNKNOWN)).toUpperCase();
		String readiness = String.valueOf(availability.getOrDefault("readiness", STATE_UNKNOWN)).toUpperCase();
		if (!"CORRECT".equals(liveness) || !"ACCEPTING_TRAFFIC".equals(readiness)) {
			overall = higherSeverity(overall, STATE_WARN);
		}

		return overall;
	}

	private String asState(Object state) {
		String value = String.valueOf(state == null ? "" : state).toUpperCase();
		if (STATE_CRITICAL.equals(value) || STATE_WARN.equals(value) || STATE_IDLE.equals(value) || STATE_UNKNOWN.equals(value)) {
			return value;
		}
		return STATE_HEALTHY;
	}

	private Double readGauge(String metricName, String... tags) {
		if (meterRegistry == null) {
			return null;
		}

		Collection<Gauge> gauges = meterRegistry.find(metricName).tags(tags).gauges();
		if (!gauges.isEmpty()) {
			return gauges.stream().mapToDouble(Gauge::value).sum();
		}

		Gauge gauge = meterRegistry.find(metricName).gauge();
		return gauge == null ? null : gauge.value();
	}

	private Double readMeterSum(String metricName, Statistic statistic, String... tags) {
		if (meterRegistry == null) {
			return null;
		}

		Collection<Meter> meters = meterRegistry.find(metricName).tags(tags).meters();
		if (meters.isEmpty()) {
			return null;
		}

		double total = 0d;
		boolean found = false;
		for (Meter meter : meters) {
			for (Measurement measurement : meter.measure()) {
				if (measurement.getStatistic() == statistic) {
					total += measurement.getValue();
					found = true;
				}
			}
		}

		return found ? total : null;
	}

	private double percentage(Double numerator, Double denominator) {
		if (denominator == null || denominator <= 0d || numerator == null) {
			return 0d;
		}
		return (numerator / denominator) * 100d;
	}

	private double coalesce(Double value) {
		return value == null ? 0d : value;
	}
}

