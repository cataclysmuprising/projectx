package com.github.projectx.backend.config.cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;

/**
 * Fail-open cache error handler.
 * <p>
 * Cache failures are logged but never propagated to business flow.
 */
public class CacheFailOpenErrorHandler extends SimpleCacheErrorHandler {
	private static final Logger log = LogManager.getLogger("applicationLogs.cache");
	private static final int MAX_KEY_LOG_CHARS = 120;

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public CacheFailOpenErrorHandler() {
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	public void handleCacheGetError(RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
		log.warn("Cache GET failed cache={} key={} error={}", cacheName(cache), safeKey(key), exception.toString());
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	public void handleCachePutError(RuntimeException exception, @NonNull Cache cache, @NonNull Object key, Object value) {
		log.warn("Cache PUT failed cache={} key={} error={}", cacheName(cache), safeKey(key), exception.toString());
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	public void handleCacheEvictError(RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
		log.warn("Cache EVICT failed cache={} key={} error={}", cacheName(cache), safeKey(key), exception.toString());
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	public void handleCacheClearError(RuntimeException exception, @NonNull Cache cache) {
		log.warn("Cache CLEAR failed cache={} error={}", cacheName(cache), exception.toString());
	}

	private String cacheName(Cache cache) {
		return cache == null ? "unknown" : cache.getName();
	}

	private String safeKey(Object key) {
		if (key == null) {
			return "null";
		}
		String text = String.valueOf(key);
		if (text.length() <= MAX_KEY_LOG_CHARS) {
			return text;
		}
		return text.substring(0, MAX_KEY_LOG_CHARS) + "...";
	}
}
