package com.github.projectx.backend.config.cache;

import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;

import java.util.*;

/**
 * Selects cache manager per cache-name based on TTL configuration.
 * <p>
 * Cache names present in TTL map are routed to TTL manager; others use
 * permanent manager.
 */
public class CacheRoutingResolver implements CacheResolver {
	private final CacheManager permanentCacheManager;
	private final CacheManager ttlCacheManager;
	private final Map<String, java.time.Duration> ttlMap;

	/**
	 * Stores immutable routing rules so cache selection stays deterministic across all cacheable
	 * service methods.
	 */
	public CacheRoutingResolver(
			CacheManager permanentCacheManager,
			CacheManager ttlCacheManager,
			Map<String, java.time.Duration> ttlMap
	) {
		this.permanentCacheManager = permanentCacheManager;
		this.ttlCacheManager = ttlCacheManager;
		this.ttlMap = ttlMap == null ? Map.of() : Collections.unmodifiableMap(ttlMap);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	@NonNull
	public Collection<? extends Cache> resolveCaches(@NonNull CacheOperationInvocationContext<?> context) {
		List<Cache> caches = new ArrayList<>();
		for (String cacheName : context.getOperation().getCacheNames()) {
			CacheManager manager = ttlMap.containsKey(cacheName) ? ttlCacheManager : permanentCacheManager;
			Cache cache = manager.getCache(cacheName);
			if (cache != null) {
				caches.add(cache);
			}
		}
		return caches;
	}
}
