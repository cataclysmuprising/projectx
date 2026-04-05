package com.github.projectx.backend.config.web;

import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.web.exchanges.HttpExchangeRepository;
import org.springframework.boot.actuate.web.exchanges.InMemoryHttpExchangeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import javax.sql.DataSource;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized observability customization.
 * <p>
 * We keep this class minimal and vendor-neutral:
 * - adds stable common metric tags
 * - avoids framework-internal APIs that may change across Boot upgrades
 */
@Configuration
public class ObservabilityConfig {

	private final Environment environment;

	public ObservabilityConfig(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Adds a normalized profile tag to every emitted meter.
	 */
	@Bean
	public MeterFilter commonTagsMeterFilter() {
		return MeterFilter.commonTags(List.of(Tag.of("profile", resolveProfileTag())));
	}

	/**
	 * Prevents runaway metric cardinality due to dynamic URI patterns.
	 */
	@Bean
	public MeterFilter httpServerUriCardinalityGuard(
			@Value("${management.metrics.http.server.max-uri-tags:250}") int maxUriTags
	) {
		return MeterFilter.maximumAllowableTags(
				"http.server.requests",
				"uri",
				Math.max(50, maxUriTags),
				MeterFilter.deny()
		);
	}

	/**
	 * Required by Actuator `httpexchanges` endpoint to retain recent exchanges.
	 */
	@Bean
	@ConditionalOnProperty(
			value = "management.httpexchanges.recording.enabled",
			havingValue = "true"
	)
	public HttpExchangeRepository httpExchangeRepository(
			@Value("${management.httpexchanges.recording.capacity:500}") int exchangeCapacity
	) {
		InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
		repository.setCapacity(Math.max(100, exchangeCapacity));
		return repository;
	}

	/**
	 * Adds production-focused runtime context to `/actuator/info`.
	 */
	@Bean
	public InfoContributor operationalInfoContributor(
			ObjectProvider<DataSource> dataSourceProvider,
			ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
			ObjectProvider<BuildProperties> buildPropertiesProvider
	) {
		return builder -> {
			builder.withDetail("app", appDetails());
			builder.withDetail("build", buildDetails(buildPropertiesProvider.getIfAvailable()));
			builder.withDetail("runtime", runtimeDetails());
			builder.withDetail("datasource", datasourceDetails(dataSourceProvider.getIfAvailable()));
			builder.withDetail("redis", redisDetails(redisConnectionFactoryProvider.getIfAvailable()));
		};
	}

	private Map<String, Object> appDetails() {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("name", environment.getProperty("spring.application.name", "projectx-backend"));
		details.put("contextPath", environment.getProperty("server.servlet.context-path", "/"));
		details.put("activeProfiles", Arrays.asList(environment.getActiveProfiles()));
		return details;
	}

	private Map<String, Object> buildDetails(BuildProperties buildProperties) {
		Map<String, Object> details = new LinkedHashMap<>();
		if (buildProperties == null) {
			details.put("version", environment.getProperty("app.version", "unknown"));
			return details;
		}

		details.put("group", buildProperties.getGroup());
		details.put("artifact", buildProperties.getArtifact());
		details.put("name", buildProperties.getName());
		details.put("version", buildProperties.getVersion());
		details.put("time", buildProperties.getTime() == null ? null : buildProperties.getTime().toString());
		return details;
	}

	private Map<String, Object> runtimeDetails() {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("javaVersion", System.getProperty("java.version", "unknown"));
		details.put("timezone", ZoneId.systemDefault().toString());
		details.put("serverTime", ZonedDateTime.now().toString());
		return details;
	}

	private Map<String, Object> datasourceDetails(DataSource dataSource) {
		Map<String, Object> details = new LinkedHashMap<>();
		if (!(dataSource instanceof HikariDataSource hikari)) {
			details.put("type", dataSource == null ? "not-configured" : dataSource.getClass().getSimpleName());
			return details;
		}

		details.put("type", hikari.getClass().getSimpleName());
		details.put("poolName", hikari.getPoolName());
		details.put("jdbcUrl", safeJdbcUrl(hikari.getJdbcUrl()));
		details.put("minIdle", hikari.getMinimumIdle());
		details.put("maxPoolSize", hikari.getMaximumPoolSize());
		if (hikari.getHikariPoolMXBean() != null) {
			details.put("activeConnections", hikari.getHikariPoolMXBean().getActiveConnections());
			details.put("idleConnections", hikari.getHikariPoolMXBean().getIdleConnections());
			details.put("threadsAwaitingConnection", hikari.getHikariPoolMXBean().getThreadsAwaitingConnection());
		}
		return details;
	}

	private Map<String, Object> redisDetails(RedisConnectionFactory redisConnectionFactory) {
		Map<String, Object> details = new LinkedHashMap<>();
		details.put("type", redisConnectionFactory == null ? "not-configured" : redisConnectionFactory.getClass().getSimpleName());
		details.put("host", environment.getProperty("spring.data.redis.host", "localhost"));
		details.put("port", environment.getProperty("spring.data.redis.port", "6379"));
		details.put("ssl", environment.getProperty("spring.data.redis.ssl.enabled", "false"));
		details.put("clientType", environment.getProperty("spring.data.redis.client-type", "lettuce"));
		return details;
	}

	private String safeJdbcUrl(String jdbcUrl) {
		if (jdbcUrl == null || jdbcUrl.isBlank()) {
			return "unknown";
		}
		int paramsIndex = jdbcUrl.indexOf('?');
		if (paramsIndex < 0) {
			return jdbcUrl;
		}
		return jdbcUrl.substring(0, paramsIndex);
	}

	private String resolveProfileTag() {
		String[] activeProfiles = environment.getActiveProfiles();
		if (activeProfiles.length == 0) {
			return "default";
		}
		return String.join(",", activeProfiles);
	}
}
