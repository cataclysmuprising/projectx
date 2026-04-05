package com.github.projectx.backend.config.cache;

import com.github.projectx.persistence.criteria.base.AbstractCriteria;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Redis cache configuration with two manager profiles:
 * <p>
 * - permanent cache manager for non-expiring caches
 * - TTL cache manager for explicitly configured expiring caches
 * <p>
 * Also defines deterministic cache-key generation for complex criteria objects.
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheTtlProperties.class)
public class RedisCacheConfig implements CachingConfigurer {

	private static final int MAX_KEY_PART_CHARS = 200;

	private final String prefixBase;
	private final ApplicationContext applicationContext;

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public RedisCacheConfig(Environment environment, ApplicationContext applicationContext) {
		prefixBase = computePrefixBase(environment);
		this.applicationContext = applicationContext;
	}

	private static String computePrefixBase(Environment environment) {
		String appName = environment.getProperty("spring.application.name", "projectx");
		String[] profiles = environment.getActiveProfiles();

		String profilePart;
		if (profiles.length == 0) {
			profilePart = "default";
		}
		else {
			StringJoiner joiner = new StringJoiner(",");
			for (String p : profiles) {
				if (p != null && !p.isBlank()) {
					joiner.add(p);
				}
			}
			profilePart = joiner.length() == 0 ? "default" : joiner.toString();
		}

		return appName + ":" + profilePart + ":";
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Bean
	public KeyGenerator keyGenerator(ObjectMapper cacheKeyObjectMapper) {

		return (target, method, params) -> {
			StringBuilder sb = new StringBuilder(160);

			sb.append(target.getClass().getSimpleName())
					.append(':')
					.append(method.getName());

			for (Object param : params) {
				if (shouldSkip(param)) {
					continue;
				}

				String part = stableKeyPart(cacheKeyObjectMapper, param);
				sb.append(':').append(part);
			}

			return sb.toString();
		};
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Bean
	public RedisCacheConfiguration redisCacheConfiguration() {
		return RedisCacheConfiguration.defaultCacheConfig()
				.disableCachingNullValues()
				.computePrefixWith(cacheName -> prefixBase + cacheName + "::")
				.serializeKeysWith(SerializationPair.fromSerializer(RedisSerializer.string()))
				.serializeValuesWith(SerializationPair.fromSerializer(RedisSerializer.json()));
	}

	/**
	 * Provides a non-expiring cache manager so reference data stays hot without TTL churn when the
	 * cache name is configured as permanent.
	 */
	@Bean
	@org.springframework.context.annotation.Primary
	public RedisCacheManager permanentCacheManager(
			RedisConnectionFactory connectionFactory,
			RedisCacheConfiguration redisCacheConfiguration
	) {
		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(redisCacheConfiguration)
				.transactionAware()
				.build();
	}

	/**
	 * Builds a dedicated TTL manager so expiring caches can be tuned per cache-name without
	 * affecting permanent cache behavior.
	 */
	@Bean
	public RedisCacheManager ttlCacheManager(
			RedisConnectionFactory connectionFactory,
			RedisCacheConfiguration redisCacheConfiguration,
			CacheTtlProperties cacheTtlProperties
	) {
		RedisCacheConfiguration ttlConfig = redisCacheConfiguration;

		if (cacheTtlProperties.getCompression().isEnabled()) {
			RedisSerializer<Object> valueSerializer = new CompressingRedisSerializer(
					RedisSerializer.json(),
					cacheTtlProperties.getCompression().getMinSize()
			);
			ttlConfig = redisCacheConfiguration.serializeValuesWith(
					SerializationPair.fromSerializer(valueSerializer)
			);
		}

		Map<String, RedisCacheConfiguration> perCacheTtl = new HashMap<>();
		for (Map.Entry<String, java.time.Duration> entry : cacheTtlProperties.getTtl().entrySet()) {
			java.time.Duration ttl = entry.getValue();
			if (ttl == null || ttl.isZero() || ttl.isNegative()) {
				continue;
			}
			perCacheTtl.put(entry.getKey(), ttlConfig.entryTtl(ttl));
		}

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(ttlConfig)
				.withInitialCacheConfigurations(perCacheTtl)
				.transactionAware()
				.build();
	}

	/**
	 * Routes each cache operation to the correct manager so TTL and permanent caches can coexist
	 * with one consistent `@Cacheable` API.
	 */
	@Bean
	public CacheResolver cacheResolver(
			@Qualifier("permanentCacheManager") RedisCacheManager permanentManager,
			@Qualifier("ttlCacheManager") RedisCacheManager ttlManager,
			CacheTtlProperties cacheTtlProperties
	) {
		return new CacheRoutingResolver(permanentManager, ttlManager, cacheTtlProperties.getTtl());
	}

	@Override
	public KeyGenerator keyGenerator() {
		return applicationContext.getBean("keyGenerator", KeyGenerator.class);
	}

	@Override
	public CacheResolver cacheResolver() {
		return applicationContext.getBean("cacheResolver", CacheResolver.class);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Bean
	@Override
	public CacheErrorHandler errorHandler() {
		return new CacheFailOpenErrorHandler();
	}

	private String stableKeyPart(ObjectMapper mapper, Object param) {
		if (param == null) {
			return "null";
		}

		if (param instanceof AbstractCriteria<?> c) {
			return hashIfTooLong(serializeCanonical(mapper, c));
		}

		if (param instanceof Number
				|| param instanceof Boolean
				|| param instanceof Enum<?>
				|| param instanceof CharSequence) {
			return hashIfTooLong(param.toString());
		}

		if (param.getClass().isArray()) {
			int len = Array.getLength(param);
			List<Object> list = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				list.add(canonicalize(Array.get(param, i)));
			}
			return hashIfTooLong(serializeCanonical(mapper, list));
		}

		if (param instanceof Collection<?> col) {
			List<Object> list = new ArrayList<>(col.size());
			for (Object v : col) {
				list.add(canonicalize(v));
			}
			return hashIfTooLong(serializeCanonical(mapper, list));
		}

		if (param instanceof Map<?, ?> map) {
			return hashIfTooLong(serializeCanonical(mapper, canonicalizeMap(map)));
		}

		return hashIfTooLong(serializeCanonical(mapper, param));
	}

	/**
	 * Canonicalize nested objects into deterministic Map/List/primitives where possible.
	 */
	private Object canonicalize(Object v) {
		if (v == null) {
			return null;
		}

		if (v instanceof Number || v instanceof Boolean || v instanceof Enum<?> || v instanceof CharSequence) {
			return v.toString();
		}

		if (v.getClass().isArray()) {
			int len = Array.getLength(v);
			List<Object> list = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				list.add(canonicalize(Array.get(v, i)));
			}
			return list;
		}

		if (v instanceof Collection<?> col) {
			List<Object> list = new ArrayList<>(col.size());
			for (Object e : col) {
				list.add(canonicalize(e));
			}
			return list;
		}

		if (v instanceof Map<?, ?> map) {
			return canonicalizeMap(map);
		}

		return v;
	}

	/**
	 * Deterministic map canonicalization: sort by String value of key.
	 * Avoids TreeMap comparability crashes.
	 */
	private Map<String, Object> canonicalizeMap(Map<?, ?> map) {
		if (map.isEmpty()) {
			return Map.of();
		}

		List<Map.Entry<?, ?>> entries = new ArrayList<>(map.entrySet());
		entries.sort(Comparator.comparing(a -> String.valueOf(a.getKey())));

		Map<String, Object> out = new LinkedHashMap<>(entries.size());
		for (Map.Entry<?, ?> e : entries) {
			String k = String.valueOf(e.getKey());
			out.put(k, canonicalize(e.getValue()));
		}
		return out;
	}

	private String serializeCanonical(ObjectMapper mapper, Object value) {
		try {
			return mapper.writeValueAsString(value);
		}
		catch (Exception e) {
			return String.valueOf(value);
		}
	}

	private String hashIfTooLong(String s) {
		if (s == null) {
			return "null";
		}
		return (s.length() > MAX_KEY_PART_CHARS) ? DigestUtils.sha256Hex(s) : s;
	}

	private boolean shouldSkip(Object param) {
		switch (param) {
			case null -> {
				return true;
			}
			case AbstractCriteria<?> ignored -> {
				return false;
			}
			case String s -> {
				return s.isBlank();
			}
			case Collection<?> c -> {
				return c.isEmpty();
			}
			case Map<?, ?> m -> {
				return m.isEmpty();
			}
			default -> {
			}
		}

		if (param.getClass().isArray()) {
			return Array.getLength(param) == 0;
		}
		return false;
	}
}
