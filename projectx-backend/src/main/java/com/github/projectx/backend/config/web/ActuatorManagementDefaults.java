package com.github.projectx.backend.config.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Centralized default properties for Actuator and observability.
 * External configuration still overrides these defaults.
 */
public final class ActuatorManagementDefaults {

	private ActuatorManagementDefaults() {
	}

	public static Map<String, Object> asMap() {
		Map<String, Object> properties = new LinkedHashMap<>();

		properties.put("management.endpoints.enabled-by-default", false);
		properties.put("management.endpoints.web.base-path", "/actuator");
		properties.put("management.endpoints.web.discovery.enabled", true);
		properties.put(
				"management.endpoints.web.exposure.include",
				"health,info,prometheus,metrics,threaddump,loggers,caches,scheduledtasks,mappings,beans,conditions,configprops,env,projectx"
		);
		properties.put("management.endpoints.jmx.exposure.exclude", "*");

		properties.put("management.endpoint.health.enabled", true);
		properties.put("management.endpoint.health.probes.enabled", true);
		properties.put("management.endpoint.health.group.readiness.include", "readinessState,db,redis,ping,diskSpace");
		properties.put("management.endpoint.health.group.liveness.include", "livenessState,ping");
		properties.put("management.endpoint.health.show-components", "when_authorized");
		properties.put("management.endpoint.health.show-details", "when_authorized");
		properties.put("management.endpoint.health.cache.time-to-live", "10s");

		properties.put("management.endpoint.info.enabled", true);
		properties.put("management.endpoint.info.cache.time-to-live", "30s");
		properties.put("management.endpoint.prometheus.enabled", true);
		properties.put("management.endpoint.metrics.enabled", true);
		properties.put("management.endpoint.metrics.cache.time-to-live", "10s");
		properties.put("management.endpoint.threaddump.enabled", true);
		properties.put("management.endpoint.loggers.enabled", true);
		properties.put("management.endpoint.caches.enabled", true);
		properties.put("management.endpoint.scheduledtasks.enabled", true);
		properties.put("management.endpoint.httpexchanges.enabled", false);
		properties.put("management.endpoint.mappings.enabled", true);
		properties.put("management.endpoint.beans.enabled", true);
		properties.put("management.endpoint.conditions.enabled", true);
		properties.put("management.endpoint.configprops.enabled", true);
		properties.put("management.endpoint.configprops.show-values", "when_authorized");
		properties.put("management.endpoint.env.enabled", true);
		properties.put("management.endpoint.env.show-values", "when_authorized");
		properties.put("management.endpoint.env.keys-to-sanitize", "password,secret,token,key,credentials");
		properties.put("management.endpoint.startup.enabled", false);
		properties.put("management.endpoint.projectx.enabled", true);

		properties.put("management.info.env.enabled", false);

		properties.put("management.metrics.tags.application", "${spring.application.name}");
		properties.put("management.metrics.enable.hikaricp", true);
		properties.put("management.metrics.enable.jdbc", true);
		properties.put("management.metrics.enable.redis", true);
		properties.put("management.metrics.enable.hibernate", true);
		properties.put("management.metrics.enable.http", true);
		properties.put("management.metrics.http.server.max-uri-tags", 250);

		properties.put("management.httpexchanges.recording.enabled", false);
		properties.put("management.httpexchanges.recording.capacity", 500);
		properties.put("management.tracing.enabled", false);
		properties.put("management.tracing.sampling.probability", 0.0d);
		properties.put("management.otlp.tracing.endpoint", "${OTLP_TRACING_ENDPOINT:http://localhost:4318/v1/traces}");

		// Operational snapshot thresholds consumed by ProjectXOpsEndpoint.
		properties.put("app.actuator.thresholds.api-latency-warn-ms", 400);
		properties.put("app.actuator.thresholds.api-latency-critical-ms", 1000);
		properties.put("app.actuator.thresholds.api-server-error-warn-percent", 1.0d);
		properties.put("app.actuator.thresholds.api-server-error-critical-percent", 3.0d);
		properties.put("app.actuator.thresholds.wallet-failure-warn-percent", 2.0d);
		properties.put("app.actuator.thresholds.wallet-failure-critical-percent", 5.0d);
		properties.put("app.actuator.thresholds.db-utilization-warn-percent", 80);
		properties.put("app.actuator.thresholds.db-utilization-critical-percent", 95);
		properties.put("app.actuator.thresholds.db-pending-warn", 1);
		properties.put("app.actuator.thresholds.db-pending-critical", 10);
		properties.put("app.actuator.thresholds.redis-latency-warn-ms", 25);
		properties.put("app.actuator.thresholds.redis-latency-critical-ms", 100);
		properties.put("app.actuator.thresholds.redis-failure-warn-percent", 1.0d);
		properties.put("app.actuator.thresholds.redis-failure-critical-percent", 3.0d);
		properties.put("app.actuator.thresholds.redis-consecutive-failure-critical", 3);

		// Shared retry defaults consumed by @RedisTransientRetry.
		properties.put("app.retry.redis.max-attempts", 3);
		properties.put("app.retry.redis.delay-ms", 200);
		properties.put("app.retry.redis.multiplier", 2.0d);
		properties.put("app.retry.redis.max-delay-ms", 2000);

		return Map.copyOf(properties);
	}
}
