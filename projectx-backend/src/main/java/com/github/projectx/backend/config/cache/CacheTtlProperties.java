package com.github.projectx.backend.config.cache;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Externalized cache behavior configuration.
 * <p>
 * - {@code ttl}: per-cache TTL overrides
 * - {@code compression}: value compression thresholds
 * - {@code warmup}: startup cache priming toggle
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "cache")
public class CacheTtlProperties {
	private Map<String, Duration> ttl = new HashMap<>();
	private Compression compression = new Compression();
	private Warmup warmup = new Warmup();

	@Setter
	@Getter
	public static class Compression {
		private boolean enabled = true;
		private int minSize = 1024;
	}

	@Setter
	@Getter
	public static class Warmup {
		private boolean enabled = false;
	}
}
