package com.github.projectx.backend.service.actuator;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.RedisConnectionCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Emits deterministic Redis command metrics to make `/actuator/metrics/redis.command`
 * always available for operational dashboards.
 */
@Service
public class RedisCommandObservabilityService {

	private static final Logger logger =
			LogManager.getLogger("serviceLogs." + RedisCommandObservabilityService.class.getSimpleName());

	private final StringRedisTemplate redisTemplate;
	private final Timer redisCommandTimerSuccess;
	private final Timer redisCommandTimerFailure;
	private final Counter redisCommandErrorCounter;
	private final AtomicLong lastLatencyMs = new AtomicLong(-1L);
	private final AtomicLong lastSuccessEpochMs = new AtomicLong(0L);
	private final AtomicLong lastFailureEpochMs = new AtomicLong(0L);
	private final AtomicLong consecutiveFailures = new AtomicLong(0L);

	public RedisCommandObservabilityService(
			ObjectProvider<StringRedisTemplate> redisTemplateProvider,
			ObjectProvider<MeterRegistry> meterRegistryProvider
	) {
		redisTemplate = redisTemplateProvider.getIfAvailable();
		MeterRegistry registry = meterRegistryProvider.getIfAvailable();

		if (registry == null) {
			redisCommandTimerSuccess = null;
			redisCommandTimerFailure = null;
			redisCommandErrorCounter = null;
			return;
		}

		redisCommandTimerSuccess = Timer.builder("redis.command")
				.description("Latency of synthetic Redis PING command")
				.tag("command", "PING")
				.tag("result", "success")
				.register(registry);

		redisCommandTimerFailure = Timer.builder("redis.command")
				.description("Latency of synthetic Redis PING command failures")
				.tag("command", "PING")
				.tag("result", "failure")
				.register(registry);

		redisCommandErrorCounter = Counter.builder("redis.command.errors")
				.description("Total Redis command probe failures")
				.tag("command", "PING")
				.register(registry);

		Gauge.builder("redis.command.last.latency.ms", lastLatencyMs, AtomicLong::doubleValue)
				.description("Latency in milliseconds for the most recent successful Redis PING")
				.register(registry);

		Gauge.builder("redis.command.last.success.epoch.ms", lastSuccessEpochMs, AtomicLong::doubleValue)
				.description("Epoch milliseconds of the most recent successful Redis PING")
				.register(registry);

		Gauge.builder("redis.command.last.failure.epoch.ms", lastFailureEpochMs, AtomicLong::doubleValue)
				.description("Epoch milliseconds of the most recent failed Redis PING")
				.register(registry);

		Gauge.builder("redis.command.consecutive.failures", consecutiveFailures, AtomicLong::doubleValue)
				.description("Current consecutive Redis PING failure count")
				.register(registry);
	}

	public void probeRedisPing() {
		if (redisTemplate == null || redisCommandTimerSuccess == null) {
			return;
		}

		long startedNs = System.nanoTime();
		try {
			String pong = redisTemplate.execute(RedisConnectionCommands::ping);
			long elapsedNs = System.nanoTime() - startedNs;
			redisCommandTimerSuccess.record(elapsedNs, TimeUnit.NANOSECONDS);
			lastLatencyMs.set(TimeUnit.NANOSECONDS.toMillis(elapsedNs));
			lastSuccessEpochMs.set(System.currentTimeMillis());
			consecutiveFailures.set(0L);

			if (pong == null || pong.isBlank()) {
				logger.warn("[redis-metrics] ping returned empty response");
			}
		}
		catch (Exception e) {
			long elapsedNs = System.nanoTime() - startedNs;
			if (redisCommandTimerFailure != null) {
				redisCommandTimerFailure.record(elapsedNs, TimeUnit.NANOSECONDS);
			}
			if (redisCommandErrorCounter != null) {
				redisCommandErrorCounter.increment();
			}
			lastFailureEpochMs.set(System.currentTimeMillis());
			consecutiveFailures.incrementAndGet();
			logger.warn("[redis-metrics] ping probe failed", e);
		}
	}
}
