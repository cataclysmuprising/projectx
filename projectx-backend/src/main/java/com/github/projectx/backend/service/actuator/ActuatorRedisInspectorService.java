package com.github.projectx.backend.service.actuator;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisKeyCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class ActuatorRedisInspectorService {

	private static final Logger logger =
			LogManager.getLogger("serviceLogs." + ActuatorRedisInspectorService.class.getSimpleName());

	private static final int DEFAULT_KEY_LIMIT = 100;
	private static final int MAX_KEY_LIMIT = 500;
	private static final int MAX_SCAN_ITERATIONS = 20_000;
	private static final int MAX_DELETE_BATCH = 200;
	private static final int BULK_DELETE_CONFIRMATION_THRESHOLD = 20;
	private static final int MAX_PATTERN_LENGTH = 160;
	private static final int MAX_KEY_LENGTH = 512;
	private static final int MAX_INVALID_KEY_REPORT = 20;

	private final RedisConnectionFactory redisConnectionFactory;
	private final StringRedisTemplate redisTemplate;

	public ActuatorRedisInspectorService(
			ObjectProvider<RedisConnectionFactory> redisConnectionFactoryProvider,
			ObjectProvider<StringRedisTemplate> redisTemplateProvider
	) {
		redisConnectionFactory = redisConnectionFactoryProvider.getIfAvailable();
		redisTemplate = redisTemplateProvider.getIfAvailable();
	}

	public Map<String, Object> summary() {
		Map<String, Object> response = baseResponse();

		if (!isRedisReady()) {
			return unavailableResponse(response, "Redis connection is not configured.");
		}

		try (RedisConnection connection = redisConnectionFactory.getConnection()) {
			Properties info = safeInfo(connection);

			Long usedMemoryBytes = parseLong(info.getProperty("used_memory"));
			Long configuredMaxMemoryBytes = parseLong(info.getProperty("maxmemory"));
			Long totalSystemMemoryBytes = parseLong(info.getProperty("total_system_memory"));
			String memoryLimitSource = resolveMemoryLimitSource(configuredMaxMemoryBytes, totalSystemMemoryBytes);
			Long effectiveMemoryLimitBytes = resolveMemoryLimitBytes(configuredMaxMemoryBytes, totalSystemMemoryBytes);
			Long remainingMemoryBytes = computeRemainingMemory(usedMemoryBytes, effectiveMemoryLimitBytes);
			Double usedPercent = computeUsagePercent(usedMemoryBytes, effectiveMemoryLimitBytes);

			Map<String, Object> memory = new LinkedHashMap<>();
			memory.put("usedBytes", usedMemoryBytes);
			memory.put("usedHuman", firstNonBlank(info.getProperty("used_memory_human"), formatBytes(usedMemoryBytes)));
			memory.put("maxBytes", effectiveMemoryLimitBytes);
			memory.put("maxHuman", resolveMemoryLimitHuman(
					memoryLimitSource,
					configuredMaxMemoryBytes,
					totalSystemMemoryBytes,
					info.getProperty("maxmemory_human"),
					info.getProperty("total_system_memory_human")
			));
			memory.put("configuredMaxBytes", configuredMaxMemoryBytes);
			memory.put("configuredMaxHuman", formatMaxMemory(configuredMaxMemoryBytes, info.getProperty("maxmemory_human")));
			memory.put("totalSystemMemoryBytes", totalSystemMemoryBytes);
			memory.put("totalSystemMemoryHuman", firstNonBlank(
					info.getProperty("total_system_memory_human"),
					formatBytes(totalSystemMemoryBytes)
			));
			memory.put("remainingBytes", remainingMemoryBytes);
			memory.put("remainingHuman", resolveRemainingHuman(remainingMemoryBytes, memoryLimitSource));
			memory.put("usedPercent", usedPercent);
			memory.put("limitSource", memoryLimitSource);

			Long dbSize = safeDbSize(connection);
			Long expiringKeys = sumKeyspaceValue(info, "expires");
			Long avgTtlMs = weightedKeyspaceAverageTtl(info);
			Long keyspaceHits = parseLong(info.getProperty("keyspace_hits"));
			Long keyspaceMisses = parseLong(info.getProperty("keyspace_misses"));

			Map<String, Object> keys = new LinkedHashMap<>();
			keys.put("totalKeys", dbSize);
			keys.put("expiringKeys", expiringKeys);
			keys.put("avgTtlMs", avgTtlMs);
			keys.put("hitRatePercent", computeHitRatePercent(keyspaceHits, keyspaceMisses));
			keys.put("keyspaceHits", keyspaceHits);
			keys.put("keyspaceMisses", keyspaceMisses);
			keys.put("keyspace", parseKeyspace(info));

			Map<String, Object> server = new LinkedHashMap<>();
			server.put("redisVersion", info.getProperty("redis_version"));
			server.put("role", info.getProperty("role"));
			server.put("uptimeSeconds", parseLong(info.getProperty("uptime_in_seconds")));
			server.put("connectedClients", parseLong(info.getProperty("connected_clients")));
			server.put("blockedClients", parseLong(info.getProperty("blocked_clients")));
			server.put("instantaneousOpsPerSec", parseLong(info.getProperty("instantaneous_ops_per_sec")));
			server.put("totalConnectionsReceived", parseLong(info.getProperty("total_connections_received")));
			server.put("rejectedConnections", parseLong(info.getProperty("rejected_connections")));
			server.put("evictedKeys", parseLong(info.getProperty("evicted_keys")));
			server.put("expiredKeys", parseLong(info.getProperty("expired_keys")));
			server.put("memFragmentationRatio", parseDouble(info.getProperty("mem_fragmentation_ratio")));

			response.put("memory", memory);
			response.put("keys", keys);
			response.put("server", server);
			return response;
		}
		catch (Exception e) {
			logger.warn("[actuator-redis] failed to load redis summary", e);
			return unavailableResponse(response, "Failed to query Redis summary.");
		}
	}

	public Map<String, Object> searchKeys(String pattern, Integer limit) {
		Map<String, Object> response = baseResponse();
		int effectiveLimit = sanitizeLimit(limit);
		String normalizedPattern;
		try {
			normalizedPattern = normalizePattern(pattern);
		}
		catch (IllegalArgumentException e) {
			response.put("pattern", StringUtils.defaultString(pattern));
			response.put("limit", effectiveLimit);
			return unavailableResponse(response, e.getMessage());
		}

		response.put("pattern", normalizedPattern);
		response.put("limit", effectiveLimit);

		if (!isRedisReady()) {
			return unavailableResponse(response, "Redis connection is not configured.");
		}

		List<Map<String, Object>> rows = new ArrayList<>();
		boolean hasMore;
		int scanCount = Math.max(100, effectiveLimit * 4);

		try (RedisConnection connection = redisConnectionFactory.getConnection()) {
			RedisKeyCommands keyCommands = connection.keyCommands();

			ScanOptions scanOptions = ScanOptions.scanOptions()
					.match(normalizedPattern)
					.count(scanCount)
					.build();

			try (Cursor<byte[]> cursor = keyCommands.scan(scanOptions)) {
				int iterations = 0;
				while (cursor.hasNext() && rows.size() < effectiveLimit && iterations < MAX_SCAN_ITERATIONS) {
					iterations += 1;
					String key = decodeKey(cursor.next());
					if (StringUtils.isBlank(key)) {
						continue;
					}
					rows.add(buildKeyRow(key));
				}
				hasMore = cursor.hasNext();
			}
		}
		catch (Exception e) {
			logger.warn("[actuator-redis] failed to scan redis keys pattern={} limit={}", normalizedPattern, effectiveLimit, e);
			return unavailableResponse(response, "Failed to query Redis keys.");
		}

		rows.sort(Comparator.comparing(
				row -> String.valueOf(row.getOrDefault("key", "")),
				String.CASE_INSENSITIVE_ORDER
		));

		response.put("returned", rows.size());
		response.put("hasMore", hasMore);
		response.put("keys", rows);
		return response;
	}

	public Map<String, Object> deleteKeys(Collection<String> keys, String confirmation) {
		Map<String, Object> response = baseResponse();

		if (!isRedisReady()) {
			return unavailableResponse(response, "Redis connection is not configured.");
		}

		int requested = countNonBlankKeys(keys);
		NormalizedKeys normalizedKeys = normalizeKeys(keys);
		List<String> acceptedKeys = normalizedKeys.keys();
		response.put("requested", requested);
		response.put("accepted", acceptedKeys.size());

		if (!normalizedKeys.invalidKeys().isEmpty()) {
			response.put("invalidKeys", normalizedKeys.invalidKeys());
		}
		if (normalizedKeys.truncatedByLimit()) {
			response.put("truncated", true);
		}

		if (acceptedKeys.isEmpty()) {
			response.put("deleted", 0);
			response.put("message", "No valid Redis keys were provided.");
			return response;
		}

		if (acceptedKeys.size() >= BULK_DELETE_CONFIRMATION_THRESHOLD) {
			String expected = requiredConfirmationValue(acceptedKeys.size());
			String submitted = StringUtils.trimToEmpty(confirmation);
			if (!expected.equals(submitted)) {
				response.put("deleted", 0);
				response.put("status", "WARN");
				response.put("confirmationRequired", true);
				response.put("confirmationHint", expected);
				response.put("message", "Bulk delete requires confirmation.");
				response.put("keys", previewKeys(acceptedKeys));
				return response;
			}
		}

		if (normalizedKeys.truncatedByLimit()) {
			response.put("message", "Delete request exceeded the batch limit. Only first " + MAX_DELETE_BATCH + " keys were processed.");
		}
		else {
			response.put("message", "Redis key deletion completed.");
		}

		try {
			Long deleted = redisTemplate.delete(acceptedKeys);
			int deletedCount = deleted == null ? 0 : deleted.intValue();
			response.put("deleted", deletedCount);
			response.put("notFound", Math.max(0, acceptedKeys.size() - deletedCount));
			response.put("keys", acceptedKeys);
			return response;
		}
		catch (Exception e) {
			logger.warn("[actuator-redis] failed to delete redis keys count={}", acceptedKeys.size(), e);
			return unavailableResponse(response, "Failed to delete selected Redis keys.");
		}
	}

	private Map<String, Object> buildKeyRow(String key) {
		Map<String, Object> row = new LinkedHashMap<>();
		Long ttlSeconds = safeGetExpireSeconds(key);

		row.put("key", key);
		row.put("dataType", resolveDataType(key));
		row.put("ttlSeconds", ttlSeconds);
		row.put("ttlLabel", formatTtlLabel(ttlSeconds));
		row.put("expireAt", computeExpireAt(ttlSeconds));
		row.put("persistent", ttlSeconds != null && ttlSeconds == -1L);
		return row;
	}

	private Map<String, Object> parseKeyspace(Properties info) {
		Map<String, Object> keyspace = new LinkedHashMap<>();
		for (String propertyName : info.stringPropertyNames()) {
			if (!propertyName.startsWith("db")) {
				continue;
			}
			String raw = StringUtils.defaultString(info.getProperty(propertyName));
			keyspace.put(propertyName, parseSectionStats(raw));
		}
		return keyspace;
	}

	private Long sumKeyspaceValue(Properties info, String key) {
		long total = 0L;
		boolean found = false;
		for (String propertyName : info.stringPropertyNames()) {
			if (!propertyName.startsWith("db")) {
				continue;
			}
			Map<String, Long> stats = parseSectionStats(StringUtils.defaultString(info.getProperty(propertyName)));
			Long value = stats.get(key);
			if (value == null) {
				continue;
			}
			total += value;
			found = true;
		}
		return found ? total : null;
	}

	private Long weightedKeyspaceAverageTtl(Properties info) {
		long totalKeys = 0L;
		long weightedTtl = 0L;

		for (String propertyName : info.stringPropertyNames()) {
			if (!propertyName.startsWith("db")) {
				continue;
			}

			Map<String, Long> stats = parseSectionStats(StringUtils.defaultString(info.getProperty(propertyName)));
			Long keys = stats.get("keys");
			Long avgTtl = stats.get("avg_ttl");
			if (keys == null || keys <= 0 || avgTtl == null) {
				continue;
			}

			totalKeys += keys;
			weightedTtl += keys * avgTtl;
		}

		if (totalKeys <= 0) {
			return null;
		}
		return weightedTtl / totalKeys;
	}

	private Map<String, Long> parseSectionStats(String raw) {
		Map<String, Long> stats = new LinkedHashMap<>();
		if (StringUtils.isBlank(raw)) {
			return stats;
		}

		String[] pairs = raw.split(",");
		for (String pair : pairs) {
			if (StringUtils.isBlank(pair) || !pair.contains("=")) {
				continue;
			}
			String[] kv = pair.split("=", 2);
			if (kv.length != 2) {
				continue;
			}
			Long value = parseLong(kv[1]);
			if (value != null) {
				stats.put(kv[0], value);
			}
		}
		return stats;
	}

	private Properties safeInfo(RedisConnection connection) {
		if (connection == null) {
			return new Properties();
		}
		Properties info = connection.serverCommands().info();
		return info == null ? new Properties() : info;
	}

	private Long safeDbSize(RedisConnection connection) {
		try {
			if (connection == null) {
				return null;
			}
			return connection.serverCommands().dbSize();
		}
		catch (Exception e) {
			logger.debug("[actuator-redis] failed to read db size", e);
			return null;
		}
	}

	private Long safeGetExpireSeconds(String key) {
		try {
			if (redisTemplate == null || StringUtils.isBlank(key)) {
				return null;
			}
			return redisTemplate.getExpire(key, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			logger.debug("[actuator-redis] failed to read ttl for key={}", key, e);
			return null;
		}
	}

	private boolean isRedisReady() {
		return redisConnectionFactory != null && redisTemplate != null;
	}

	private Map<String, Object> unavailableResponse(Map<String, Object> response, String message) {
		response.put("status", "ERROR");
		response.put("message", message);
		return response;
	}

	private Map<String, Object> baseResponse() {
		Map<String, Object> response = new LinkedHashMap<>();
		response.put("status", "OK");
		response.put("timestamp", OffsetDateTime.now().toString());
		return response;
	}

	private int sanitizeLimit(Integer limit) {
		if (limit == null || limit <= 0) {
			return DEFAULT_KEY_LIMIT;
		}
		return Math.min(limit, MAX_KEY_LIMIT);
	}

	private String normalizePattern(String pattern) {
		String normalized = StringUtils.trimToEmpty(pattern);
		if (normalized.isEmpty()) {
			return "*";
		}
		if (normalized.length() > MAX_PATTERN_LENGTH) {
			throw new IllegalArgumentException("Pattern is too long.");
		}
		if (!isSafeRedisPattern(normalized)) {
			throw new IllegalArgumentException("Pattern contains unsupported characters.");
		}
		if (normalized.contains("*") || normalized.contains("?") || normalized.contains("[")) {
			return normalized;
		}
		return "*" + normalized + "*";
	}

	private NormalizedKeys normalizeKeys(Collection<String> keys) {
		if (keys == null || keys.isEmpty()) {
			return new NormalizedKeys(List.of(), List.of(), false);
		}

		Set<String> unique = new LinkedHashSet<>();
		List<String> invalidKeys = new ArrayList<>();
		boolean truncatedByLimit = false;

		for (String key : keys) {
			String normalized = StringUtils.trimToEmpty(key);
			if (normalized.isEmpty()) {
				continue;
			}

			if (!isSafeRedisKey(normalized)) {
				if (invalidKeys.size() < MAX_INVALID_KEY_REPORT) {
					invalidKeys.add(normalized);
				}
				continue;
			}

			unique.add(normalized);
			if (unique.size() >= MAX_DELETE_BATCH) {
				truncatedByLimit = true;
				break;
			}
		}
		return new NormalizedKeys(new ArrayList<>(unique), invalidKeys, truncatedByLimit);
	}

	private int countNonBlankKeys(Collection<String> keys) {
		if (keys == null || keys.isEmpty()) {
			return 0;
		}
		int count = 0;
		for (String key : keys) {
			if (StringUtils.isNotBlank(key)) {
				count += 1;
			}
		}
		return count;
	}

	private boolean isSafeRedisPattern(String pattern) {
		if (StringUtils.isBlank(pattern)) {
			return true;
		}
		if (pattern.length() > MAX_PATTERN_LENGTH) {
			return false;
		}
		for (int i = 0; i < pattern.length(); i++) {
			char ch = pattern.charAt(i);
			if (Character.isISOControl(ch) || Character.isWhitespace(ch)) {
				return false;
			}
		}
		return true;
	}

	private boolean isSafeRedisKey(String key) {
		if (StringUtils.isBlank(key) || key.length() > MAX_KEY_LENGTH) {
			return false;
		}
		for (int i = 0; i < key.length(); i++) {
			char ch = key.charAt(i);
			if (Character.isISOControl(ch) || Character.isWhitespace(ch)) {
				return false;
			}
		}
		return !key.contains("*") && !key.contains("?");
	}

	private String requiredConfirmationValue(int keyCount) {
		return "DELETE " + keyCount;
	}

	private List<String> previewKeys(List<String> keys) {
		if (keys == null || keys.isEmpty()) {
			return List.of();
		}
		int max = Math.min(5, keys.size());
		return keys.subList(0, max);
	}

	private Long parseLong(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		try {
			return Long.parseLong(value.trim());
		}
		catch (NumberFormatException ignore) {
			return null;
		}
	}

	private Double parseDouble(String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		try {
			return Double.parseDouble(value.trim());
		}
		catch (NumberFormatException ignore) {
			return null;
		}
	}

	private Long computeRemainingMemory(Long usedBytes, Long maxBytes) {
		if (usedBytes == null || maxBytes == null || maxBytes <= 0) {
			return null;
		}
		long remaining = maxBytes - usedBytes;
		return Math.max(0L, remaining);
	}

	private Double computeUsagePercent(Long usedBytes, Long maxBytes) {
		if (usedBytes == null || maxBytes == null || maxBytes <= 0) {
			return null;
		}
		return (double) usedBytes * 100.0 / (double) maxBytes;
	}

	private String formatMaxMemory(Long maxBytes, String maxHumanFromInfo) {
		if (maxBytes == null || maxBytes <= 0) {
			return "Unlimited";
		}
		return firstNonBlank(maxHumanFromInfo, formatBytes(maxBytes));
	}

	private Long resolveMemoryLimitBytes(Long configuredMaxBytes, Long totalSystemMemoryBytes) {
		if (configuredMaxBytes != null && configuredMaxBytes > 0) {
			return configuredMaxBytes;
		}
		if (totalSystemMemoryBytes != null && totalSystemMemoryBytes > 0) {
			return totalSystemMemoryBytes;
		}
		return null;
	}

	private String resolveMemoryLimitSource(Long configuredMaxBytes, Long totalSystemMemoryBytes) {
		if (configuredMaxBytes != null && configuredMaxBytes > 0) {
			return "REDIS_MAXMEMORY";
		}
		if (totalSystemMemoryBytes != null && totalSystemMemoryBytes > 0) {
			return "SYSTEM_MEMORY";
		}
		return "UNLIMITED";
	}

	private String resolveMemoryLimitHuman(
			String source,
			Long configuredMaxBytes,
			Long totalSystemMemoryBytes,
			String configuredMaxHuman,
			String totalSystemMemoryHuman
	) {
		if ("REDIS_MAXMEMORY".equals(source)) {
			return firstNonBlank(configuredMaxHuman, formatBytes(configuredMaxBytes));
		}
		if ("SYSTEM_MEMORY".equals(source)) {
			return firstNonBlank(totalSystemMemoryHuman, formatBytes(totalSystemMemoryBytes)) + " (system)";
		}
		return "Unlimited";
	}

	private String resolveRemainingHuman(Long remainingBytes, String source) {
		if (remainingBytes != null) {
			return formatBytes(remainingBytes);
		}
		if ("UNLIMITED".equals(source)) {
			return "Unlimited";
		}
		return "N/A";
	}

	private String formatBytes(Long bytes) {
		if (bytes == null) {
			return "N/A";
		}
		if (bytes < 1024L) {
			return bytes + " B";
		}
		double kb = bytes / 1024.0;
		if (kb < 1024.0) {
			return String.format(Locale.ROOT, "%.2f KB", kb);
		}
		double mb = kb / 1024.0;
		if (mb < 1024.0) {
			return String.format(Locale.ROOT, "%.2f MB", mb);
		}
		double gb = mb / 1024.0;
		return String.format(Locale.ROOT, "%.2f GB", gb);
	}

	private Double computeHitRatePercent(Long hits, Long misses) {
		if (hits == null || misses == null) {
			return null;
		}
		long total = hits + misses;
		if (total <= 0) {
			return null;
		}
		return (double) hits * 100.0 / (double) total;
	}

	private String formatTtlLabel(Long ttlSeconds) {
		if (ttlSeconds == null) {
			return "N/A";
		}
		if (ttlSeconds == -2L) {
			return "Not Found";
		}
		if (ttlSeconds == -1L) {
			return "Persistent";
		}
		if (ttlSeconds < 0L) {
			return "N/A";
		}

		long days = ttlSeconds / 86_400;
		long hours = (ttlSeconds % 86_400) / 3_600;
		long minutes = (ttlSeconds % 3_600) / 60;
		long seconds = ttlSeconds % 60;

		List<String> parts = new ArrayList<>();
		if (days > 0) {
			parts.add(days + "d");
		}
		if (hours > 0) {
			parts.add(hours + "h");
		}
		if (minutes > 0) {
			parts.add(minutes + "m");
		}
		parts.add(seconds + "s");
		return String.join(" ", parts);
	}

	private String computeExpireAt(Long ttlSeconds) {
		if (ttlSeconds == null || ttlSeconds < 0) {
			return null;
		}
		return OffsetDateTime.ofInstant(
				Instant.ofEpochSecond(Instant.now().getEpochSecond() + ttlSeconds),
				ZoneId.systemDefault()
		).toString();
	}

	private String resolveDataType(String key) {
		try {
			if (redisTemplate == null || StringUtils.isBlank(key)) {
				return "unknown";
			}

			DataType dataType = redisTemplate.type(key);
			if (dataType == null || dataType == DataType.NONE) {
				return "none";
			}

			return dataType.code();
		}
		catch (Exception e) {
			logger.debug("[actuator-redis] failed to resolve key type key={}", key, e);
			return "unknown";
		}
	}

	private String firstNonBlank(String first, String second) {
		if (StringUtils.isNotBlank(first)) {
			return first;
		}
		return second;
	}

	private String decodeKey(byte[] raw) {
		if (raw == null || raw.length == 0) {
			return "";
		}
		return new String(raw, StandardCharsets.UTF_8);
	}

	private record NormalizedKeys(List<String> keys, List<String> invalidKeys, boolean truncatedByLimit) {
	}
}
