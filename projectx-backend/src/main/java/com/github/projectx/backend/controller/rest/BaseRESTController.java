package com.github.projectx.backend.controller.rest;

import com.github.projectx.backend.common.exception.RequestValidationException;
import com.github.projectx.backend.config.security.web.AuthenticatedClient;
import com.github.projectx.backend.controller.rest.response.CommonApiResponse;
import com.github.projectx.backend.utils.SecurityUtil;
import com.github.projectx.persistence.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.HtmlUtils;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public abstract class BaseRESTController {

	// =====================================================================
	// CONSTANTS
	// =====================================================================

	public static final String RID_HEADER = "X-Correlation-ID";
	public static final String REQUEST_ID_HEADER = "X-Request-Id";

	protected static final String ERROR_CODE_SUCCESS = "SUCCESS";
	protected static final String ERROR_CODE_BUSINESS_RULE = "BUSINESS_RULE_VIOLATION";
	protected static final String ERROR_CODE_VALIDATION = "VALIDATION_ERROR";
	protected static final String ERROR_CODE_RATE_LIMITED = "RATE_LIMITED";
	protected static final String ERROR_CODE_INTERNAL = "INTERNAL_ERROR";
	protected static final String ERROR_CODE_INVALID_REQUEST = "INVALID_REQUEST";
	protected static final String ERROR_CODE_NOT_FOUND = "NOT_FOUND";
	protected static final String ERROR_CODE_UNAUTHORIZED = "UNAUTHORIZED";
	protected static final String ERROR_CODE_FORBIDDEN = "FORBIDDEN";
	protected static final String ERROR_CODE_SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE";
	protected static final String ERROR_CODE_CONFLICT = "CONFLICT";
	protected static final Logger logger =
			LogManager.getLogger("application.API.Logs." + BaseRESTController.class.getName());
	private static final int DEFAULT_IDEMPOTENCY_TTL_MINUTES = 120;
	private static final Set<String> SENSITIVE_KEYS = Set.of(
			"password", "pin", "pinnumber", "token", "refreshtoken", "accesstoken",
			"verify", "otp", "secret", "authorization", "devicekey"
	);
	private static final Set<String> SENSITIVE_HEADER_KEYS = Set.of(
			"authorization",
			"proxy-authorization",
			"cookie",
			"set-cookie",
			"x-api-key",
			"x-auth-token",
			"x-csrf-token",
			"x-refresh-token"
	);
	private static final Locale MYANMAR_LOCALE = Locale.of("mm");
	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	protected RedisTemplate<String, String> redisTemplate;
	@Autowired
	protected PasswordEncoder passwordEncoder;
	@Autowired
	protected MessageSource messageSource;

	// =====================================================================
	// AUTH HELPERS
	// =====================================================================

	protected Long getSignInAdministratorId() {
		AuthenticatedClient admin = getSignInAdministrator();
		return admin != null ? admin.getId() : null;
	}

	protected long requireSignInAdministratorId() {
		Long administratorId = getSignInAdministratorId();
		if (administratorId == null || administratorId <= 0L) {
			throw new RequestValidationException("Authenticated administrator is required.");
		}
		return administratorId;
	}

	protected AuthenticatedClient getSignInAdministrator() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
			return null;
		}
		return mapper.convertValue(auth.getPrincipal(), AuthenticatedClient.class);
	}

	// =====================================================================
	// RESPONSE HELPERS
	// =====================================================================

//	protected <T extends CommonApiResponse> T successResponse(
//			T response,
//			String title,
//			String message
//	) {
//		response.setStatusCode(HttpStatus.OK.value());
//		response.setTitle(title);
//		response.setMessage(message);
//		return response;
//	}

	// 1️⃣ Instance-based (simple, explicit)
	protected <T extends CommonApiResponse> T successResponse(
			T response,
			String title,
			String message
	) {
		response.setStatusCode(HttpStatus.OK.value());
		response.setErrorCode(ERROR_CODE_SUCCESS);
		response.setTitle(title);
		response.setMessage(message);
		return response;
	}

	// 2️⃣ Supplier-based (BEST for generics)
	protected <T extends CommonApiResponse> T successResponse(
			Supplier<T> supplier,
			String title,
			String message
	) {
		T response = supplier.get();
		response.setStatusCode(HttpStatus.OK.value());
		response.setErrorCode(ERROR_CODE_SUCCESS);
		response.setTitle(title);
		response.setMessage(message);
		return response;
	}

	// 3️⃣ Class-based (simple DTOs only)
	protected <T extends CommonApiResponse> T successResponse(
			Class<T> responseClass,
			String title,
			String message
	) {
		try {
			T response = responseClass.getDeclaredConstructor().newInstance();
			response.setStatusCode(HttpStatus.OK.value());
			response.setErrorCode(ERROR_CODE_SUCCESS);
			response.setTitle(title);
			response.setMessage(message);
			return response;
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Failed to create response instance for " + responseClass.getName(),
					e
			);
		}
	}

	protected <T extends CommonApiResponse> T applyResponse(
			T response,
			HttpStatus status,
			String title,
			String message
	) {
		HttpStatus safeStatus = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
		response.setStatusCode(safeStatus.value());
		response.setErrorCode(safeStatus == HttpStatus.OK ? ERROR_CODE_SUCCESS : errorCodeForStatus(safeStatus));
		response.setTitle(title);
		response.setMessage(message);
		return response;
	}

	protected <T extends CommonApiResponse> ResponseEntity<?> earlyExit(
			T response,
			HttpStatus status,
			String title,
			String message,
			String rid,
			long startedNanos
	) {
		response.setStatusCode(status.value());
		response.setErrorCode(errorCodeForStatus(status));
		response.setTitle(title);
		response.setMessage(message);

		logClose(rid, currentClass(), currentMethod(), startedNanos, response, status);
		return respondWithRid(status, response, rid);
	}

	protected BusinessException businessError(String message) {
		return new BusinessException(message);
	}

	// =====================================================================
	// LOGGING
	// =====================================================================

	protected String newRid() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	protected void logOpen(
			String rid,
			String className,
			String method,
			HttpServletRequest req,
			Object body
	) {
		ThreadContext.put("rid", rid);
		ThreadContext.put("cls", className);
		ThreadContext.put("mth", method);

		String uri = req != null ? req.getRequestURI() : "(no-request)";
		String query = safeQueryDisplayValue(req);
		logger.info(
				"""
						====================[ OPEN {}#{} ]====================
						RID: {}
						URI: {}{}
						IP: {}
						User-Agent: {}
						Headers:
						{}
						Body:
						{}
						""",
				className, method, rid,
				uri,
				query,
				getClientIp(),
				getUserAgent(),
				headersAsString(req),
				safeJson(body)
		);
	}

	protected void logClose(
			String rid,
			String className,
			String method,
			long startedNanos,
			Object resultBody,
			HttpStatus status
	) {
		long tookMs = (System.nanoTime() - startedNanos) / 1_000_000;

		logger.info(
				"""
						ResultStatus: {} ({} ms)
						ResultBody:
						{}
						====================[ CLOSE {}#{} ]====================
						""",
				status.value(), tookMs, safeJson(resultBody), className, method
		);

		ThreadContext.clearAll();
	}

	private String currentClass() {
		return ThreadContext.get("cls");
	}

	private String currentMethod() {
		return ThreadContext.get("mth");
	}

	// =====================================================================
	// REQUEST UTILS
	// =====================================================================

	protected String headersAsString(HttpServletRequest req) {
		if (req == null) {
			return "(no-request)";
		}
		StringBuilder sb = new StringBuilder();
		Enumeration<String> names = req.getHeaderNames();
		if (names == null) {
			return "(no-headers)";
		}
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			String value = safeHeaderDisplayValue(name, req.getHeader(name));
			sb.append("  ").append(name).append(": ").append(value).append("\n");
		}
		return sb.toString();
	}

	protected String safeJson(Object body) {
		try {
			Object masked = maskSensitive(body);
			return masked == null ? "(null)" : mapper.writeValueAsString(masked);
		}
		catch (Exception e) {
			return body != null ? String.valueOf(body) : "(unserializable)";
		}
	}

	protected String getClientIp() {
		HttpServletRequest request = getCurrentRequest();
		if (request == null) {
			return "0.0.0.0";
		}
		return SecurityUtil.getClientIp(request);
	}

	protected String getUserAgent() {
		HttpServletRequest request = getCurrentRequest();
		return request != null ? request.getHeader("User-Agent") : null;
	}

	protected Locale resolveLocale(HttpServletRequest request) {
		String lang = null;
		if (request != null) {
			lang = request.getHeader("Language");
		}
		if (lang == null || lang.isBlank()) {
			return LocaleContextHolder.getLocale();
		}
		String normalized = lang.trim().toLowerCase(Locale.ROOT);
		if (normalized.startsWith("mm") || normalized.startsWith("my")) {
			return MYANMAR_LOCALE;
		}
		return Locale.ENGLISH;
	}

	protected String msg(String key, Object... args) {
		return msg(getCurrentRequest(), key, args);
	}

	protected String msg(HttpServletRequest request, String key, Object... args) {
		Locale locale = resolveLocale(request);
		try {
			return messageSource.getMessage(key, args, locale);
		}
		catch (Exception e) {
			return key;
		}
	}

	protected HttpServletRequest getCurrentRequest() {
		try {
			ServletRequestAttributes attrs =
					(ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			return attrs.getRequest();
		}
		catch (Exception e) {
			return null;
		}
	}

	protected <T> T mergeIntoResponse(T target, Object source) {
		mapper.updateValue(target, source);
		return target;
	}

	protected <T, U> List<U> convertList(List<T> sourceList, Class<U> targetClass) {
		if (sourceList == null || sourceList.isEmpty()) {
			return List.of();
		}
		return sourceList.stream()
				.map(source -> mapper.convertValue(source, targetClass))
				.toList();
	}

	protected String requireNonBlank(String value, String message) throws BusinessException {
		String trimmed = StringUtils.trimToEmpty(value);
		if (StringUtils.isBlank(trimmed)) {
			throw businessError(message);
		}
		return trimmed;
	}

	protected String sanitizeForClient(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		return HtmlUtils.htmlEscape(normalized);
	}

	protected String sanitizeForClientOrDefault(String value, String fallback) {
		String sanitized = sanitizeForClient(value);
		if (StringUtils.isBlank(sanitized)) {
			return fallback;
		}
		return sanitized;
	}

	protected boolean acquireProcessingLock(String key, long ttlSeconds) {
		if (StringUtils.isBlank(key)) {
			return false;
		}
		if (redisTemplate == null) {
			logger.error("Redis processing lock unavailable, refusing guarded request key={}", safeKeyToken(key));
			return false;
		}
		try {
			Boolean ok = redisTemplate.opsForValue()
					.setIfAbsent(key, "IN_PROGRESS", ttlSeconds, TimeUnit.SECONDS);
			return Boolean.TRUE.equals(ok);
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Failed to acquire redis processing lock key=" + safeKeyToken(key),
					e
			);
		}
	}

	protected void releaseProcessingLock(String key) {
		if (StringUtils.isBlank(key)) {
			return;
		}
		try {
			redisTemplate.delete(key);
		}
		catch (Exception e) {
			logger.warn("⚠️ Failed to release redis lock key={}", safeKeyToken(key), e);
		}
	}

	protected <T> ResponseEntity<T> respondWithRid(HttpStatus status, T body, String rid) {
		HttpStatus safeStatus = status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
		String safeRid = sanitizeRid(rid);
		@SuppressWarnings("unchecked")
		T safeBody = (T) sanitizeResponsePayload(body);
		return ResponseEntity.status(safeStatus)
				.header(RID_HEADER, safeRid)
				.body(safeBody);
	}

	protected String errorCodeForStatus(HttpStatus status) {
		if (status == null) {
			return ERROR_CODE_INTERNAL;
		}
		return switch (status) {
			case BAD_REQUEST -> ERROR_CODE_INVALID_REQUEST;
			case UNAUTHORIZED -> ERROR_CODE_UNAUTHORIZED;
			case FORBIDDEN -> ERROR_CODE_FORBIDDEN;
			case NOT_FOUND -> ERROR_CODE_NOT_FOUND;
			case CONFLICT -> ERROR_CODE_CONFLICT;
			case TOO_MANY_REQUESTS -> ERROR_CODE_RATE_LIMITED;
			case SERVICE_UNAVAILABLE -> ERROR_CODE_SERVICE_UNAVAILABLE;
			default -> status.is4xxClientError() ? ERROR_CODE_INVALID_REQUEST : ERROR_CODE_INTERNAL;
		};
	}

	protected RateLimitResult checkRateLimit(String key, int limit, long windowSeconds) {
		if (StringUtils.isBlank(key)) {
			return new RateLimitResult(true, limit, 0, 0);
		}
		if (redisTemplate == null) {
			logger.error("Redis rate limit store unavailable, refusing request key={}", safeKeyToken(key));
			return rateLimitUnavailable(limit, windowSeconds);
		}
		try {
			Long count = redisTemplate.opsForValue().increment(key);
			if (count != null && count == 1L) {
				redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
			}
			long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
			boolean allowed = count != null && count <= limit;
			long remaining = Math.max(0, limit - (count == null ? 0 : count));
			return new RateLimitResult(allowed, limit, remaining, ttl);
		}
		catch (Exception e) {
			logger.error("Rate limit check failed, refusing request key={}", safeKeyToken(key), e);
			return rateLimitUnavailable(limit, windowSeconds);
		}
	}

	protected Duration defaultIdempotencyTtl() {
		return Duration.ofMinutes(DEFAULT_IDEMPOTENCY_TTL_MINUTES);
	}

	protected Duration shortIdempotencyTtl() {
		return Duration.ofMinutes(2);
	}

	protected String resolveExplicitRequestTrackingId() {
		HttpServletRequest currentRequest = getCurrentRequest();
		return firstNonBlankToken(
				currentRequest == null ? null : currentRequest.getHeader(REQUEST_ID_HEADER),
				currentRequest == null ? null : currentRequest.getHeader(RID_HEADER),
				currentRequest == null ? null : currentRequest.getHeader("X-Idempotency-Key")
		);
	}

	protected String firstNonBlankToken(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (StringUtils.isNotBlank(value)) {
				return value.trim();
			}
		}
		return null;
	}

	protected String safeKeyToken(String value) {
		if (StringUtils.isBlank(value)) {
			return "na";
		}
		String normalized = value.trim().replaceAll("[^A-Za-z0-9:_-]+", "_");
		if (normalized.length() > 80) {
			return normalized.substring(0, 80);
		}
		return normalized;
	}

	protected String fingerprintKeyToken(String... values) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			if (values != null) {
				for (String value : values) {
					if (value == null) {
						digest.update((byte) 0);
						continue;
					}
					digest.update(value.trim().getBytes(StandardCharsets.UTF_8));
					digest.update((byte) 0);
				}
			}
			String fingerprint = HexFormat.of().formatHex(digest.digest());
			return fingerprint.length() <= 24 ? fingerprint : fingerprint.substring(0, 24);
		}
		catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 digest is unavailable", e);
		}
	}

	protected String fallbackIdempotencyToken(String explicitRequestTrackingId, long bucketWindowSeconds) {
		if (StringUtils.isNotBlank(explicitRequestTrackingId)) {
			return explicitRequestTrackingId.trim();
		}
		long safeWindow = bucketWindowSeconds <= 0L ? 120L : bucketWindowSeconds;
		long bucket = Instant.now().getEpochSecond() / safeWindow;
		return "auto-" + bucket;
	}

	private Duration normalizedIdempotencyTtl(Duration ttl) {
		if (ttl == null || ttl.isNegative() || ttl.isZero()) {
			return defaultIdempotencyTtl();
		}
		return ttl;
	}

	private RateLimitResult rateLimitUnavailable(int limit, long windowSeconds) {
		return new RateLimitResult(false, limit, 0, Math.max(1L, windowSeconds));
	}

	protected boolean acquireIdempotencyKey(String key) {
		return acquireIdempotencyKey(key, defaultIdempotencyTtl());
	}

	protected boolean acquireIdempotencyKey(String key, Duration ttl) {
		if (StringUtils.isBlank(key)) {
			return false;
		}
		if (redisTemplate == null) {
			logger.error("Redis idempotency store unavailable, refusing guarded request key={}", safeKeyToken(key));
			return false;
		}
		try {
			Duration effectiveTtl = normalizedIdempotencyTtl(ttl);
			Boolean ok = redisTemplate.opsForValue()
					.setIfAbsent(key, "IN_PROGRESS", effectiveTtl);
			return Boolean.TRUE.equals(ok);
		}
		catch (Exception e) {
			throw new IllegalStateException(
					"Failed to acquire idempotency key=" + safeKeyToken(key),
					e
			);
		}
	}

	protected <T> T getIdempotencyResponse(String key, Class<T> responseType) {
		if (StringUtils.isBlank(key)) {
			return null;
		}
		try {
			String json = redisTemplate.opsForValue().get(key);
			if (StringUtils.isBlank(json) || "IN_PROGRESS".equals(json)) {
				return null;
			}
			return mapper.readValue(json, responseType);
		}
		catch (Exception e) {
			logger.warn("⚠️ Failed to read idempotency response key={}", safeKeyToken(key), e);
			return null;
		}
	}

	protected boolean isIdempotencyInProgress(String key) {
		if (StringUtils.isBlank(key) || redisTemplate == null) {
			return false;
		}
		try {
			String value = redisTemplate.opsForValue().get(key);
			return "IN_PROGRESS".equals(value);
		}
		catch (Exception e) {
			logger.warn("⚠️ Failed to inspect idempotency key={}", safeKeyToken(key), e);
			return false;
		}
	}

	protected boolean isAnyIdempotencyKeyInProgress(String... keys) {
		if (keys == null || keys.length == 0) {
			return false;
		}
		Set<String> uniqueKeys = new LinkedHashSet<>();
		for (String key : keys) {
			if (StringUtils.isNotBlank(key)) {
				uniqueKeys.add(key);
			}
		}
		for (String key : uniqueKeys) {
			if (isIdempotencyInProgress(key)) {
				return true;
			}
		}
		return false;
	}

	protected boolean storeIdempotencyResponse(String key, Object response) {
		return storeIdempotencyResponse(key, response, defaultIdempotencyTtl());
	}

	protected boolean storeIdempotencyResponse(String key, Object response, Duration ttl) {
		if (StringUtils.isBlank(key) || response == null) {
			return false;
		}
		try {
			String json = mapper.writeValueAsString(response);
			redisTemplate.opsForValue().set(key, json, normalizedIdempotencyTtl(ttl));
			return true;
		}
		catch (Exception e) {
			logger.warn("⚠️ Failed to store idempotency response key={}", safeKeyToken(key), e);
			return false;
		}
	}

	protected void releaseIdempotencyKey(String key) {
		if (StringUtils.isBlank(key)) {
			return;
		}
		try {
			redisTemplate.delete(key);
		}
		catch (Exception e) {
			logger.warn("⚠️ Failed to release idempotency key={}", safeKeyToken(key), e);
		}
	}

	protected String sanitizeRid(String rid) {
		if (StringUtils.isBlank(rid)) {
			return newRid();
		}
		String normalized = rid.replaceAll("[\\p{Cntrl}\\s]+", "");
		if (StringUtils.isBlank(normalized)) {
			return newRid();
		}
		String compact = normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
		return HtmlUtils.htmlEscape(compact);
	}

	protected Object sanitizeResponsePayload(Object payload) {
		if (payload == null) {
			return null;
		}
		if (payload instanceof String value) {
			return sanitizeForClient(value);
		}
		if (payload instanceof Number || payload instanceof Boolean || payload instanceof Enum<?>) {
			return payload;
		}
		if (payload instanceof Map<?, ?> map) {
			return sanitizeResponseMap(map);
		}
		if (payload instanceof Collection<?> collection) {
			return collection.stream().map(this::sanitizeResponsePayload).toList();
		}
		if (payload.getClass().isArray()) {
			int len = Array.getLength(payload);
			List<Object> sanitized = new ArrayList<>(len);
			for (int i = 0; i < len; i++) {
				sanitized.add(sanitizeResponsePayload(Array.get(payload, i)));
			}
			return sanitized;
		}
		try {
			JavaType mapType = mapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class);
			Map<String, Object> asMap = mapper.convertValue(payload, mapType);
			Map<String, Object> sanitizedMap = sanitizeResponseMap(asMap);
			try {
				return mapper.convertValue(sanitizedMap, payload.getClass());
			}
			catch (IllegalArgumentException ignored) {
				return sanitizedMap;
			}
		}
		catch (IllegalArgumentException e) {
			return payload;
		}
	}

	private Map<String, Object> sanitizeResponseMap(Map<?, ?> map) {
		Map<String, Object> sanitized = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String key = sanitizeResponseKey(entry.getKey());
			Object value = sanitizeResponsePayload(entry.getValue());
			sanitized.put(key, value);
		}
		return sanitized;
	}

	private String sanitizeResponseKey(Object key) {
		String raw = key == null ? "" : String.valueOf(key);
		String normalized = raw.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		String compact = normalized.length() > 128 ? normalized.substring(0, 128) : normalized;
		return HtmlUtils.htmlEscape(compact);
	}

	private Object maskSensitive(Object body) {
		if (body == null) {
			return null;
		}
		if (body instanceof String || body instanceof Number || body instanceof Boolean || body instanceof Enum) {
			return body;
		}
		if (body instanceof Map<?, ?> map) {
			return maskMap(map);
		}
		if (body instanceof List<?> list) {
			return list.stream().map(this::maskSensitive).toList();
		}
		try {
			JavaType mapType = mapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class);
			Map<String, Object> asMap = mapper.convertValue(body, mapType);
			return maskMap(asMap);
		}
		catch (Exception e) {
			return "(unserializable)";
		}
	}

	private Map<String, Object> maskMap(Map<?, ?> map) {
		Map<String, Object> masked = new LinkedHashMap<>();
		for (Map.Entry<?, ?> entry : map.entrySet()) {
			String key = entry.getKey() == null ? "" : String.valueOf(entry.getKey());
			Object value = entry.getValue();
			masked.put(key, maskValue(key, value));
		}
		return masked;
	}

	private Object maskValue(String key, Object value) {
		String keyLower = key.toLowerCase(Locale.ROOT);
		if (isSensitiveKey(keyLower)) {
			return maskScalar(value);
		}
		if (keyLower.contains("phone")) {
			return maskPhone(value);
		}
		if (keyLower.contains("accountid")) {
			return maskAccount(value);
		}
		if (keyLower.contains("email")) {
			return maskEmail(value);
		}
		return maskSensitive(value);
	}

	private boolean isSensitiveKey(String keyLower) {
		for (String s : SENSITIVE_KEYS) {
			if (keyLower.contains(s)) {
				return true;
			}
		}
		return false;
	}

	private String maskScalar(Object value) {
		if (value == null) {
			return "(null)";
		}
		String v = String.valueOf(value);
		if (v.length() <= 6) {
			return "***";
		}
		return v.substring(0, 2) + "***" + v.substring(v.length() - 2);
	}

	private String maskPhone(Object value) {
		if (value == null) {
			return "(null)";
		}
		String v = String.valueOf(value);
		if (v.length() <= 4) {
			return "***";
		}
		return "***" + v.substring(v.length() - 4);
	}

	private String maskAccount(Object value) {
		if (value == null) {
			return "(null)";
		}
		String v = String.valueOf(value);
		if (v.length() <= 3) {
			return "***";
		}
		return "***" + v.substring(v.length() - 3);
	}

	private String maskEmail(Object value) {
		if (value == null) {
			return "(null)";
		}
		String v = String.valueOf(value);
		int at = v.indexOf('@');
		if (at <= 1) {
			return "***";
		}
		return v.charAt(0) + "***" + v.substring(at);
	}

	private String safeHeaderDisplayValue(String name, String value) {
		if (StringUtils.isBlank(value)) {
			return "(blank)";
		}
		String normalizedName = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
		String abbreviated = StringUtils.abbreviate(value, 120);
		if (isSensitiveHeader(normalizedName)) {
			return maskScalar(abbreviated);
		}
		return abbreviated;
	}

	private String safeQueryDisplayValue(HttpServletRequest req) {
		if (req == null || StringUtils.isBlank(req.getQueryString())) {
			return "";
		}

		String[] parts = req.getQueryString().split("&");
		StringJoiner joiner = new StringJoiner("&", "?", "");
		for (String part : parts) {
			int separator = part.indexOf('=');
			String rawKey = separator >= 0 ? part.substring(0, separator) : part;
			String rawValue = separator >= 0 ? part.substring(separator + 1) : "";
			String safeKey = sanitizeForClientOrDefault(rawKey, "param");
			String normalizedKey = rawKey.trim().toLowerCase(Locale.ROOT);
			String safeValue = isSensitiveKey(normalizedKey) || isSensitiveHeader(normalizedKey)
					? maskScalar(rawValue)
					: normalizedKey.contains("phone")
					? maskPhone(rawValue)
					: normalizedKey.contains("email")
					? maskEmail(rawValue)
					: normalizedKey.contains("account")
					? maskAccount(rawValue)
					: StringUtils.abbreviate(
					sanitizeForClientOrDefault(rawValue, "(blank)"),
					120
			);
			joiner.add(separator >= 0 ? safeKey + "=" + safeValue : safeKey);
		}
		return joiner.toString();
	}

	private boolean isSensitiveHeader(String headerName) {
		if (StringUtils.isBlank(headerName)) {
			return false;
		}
		if (SENSITIVE_HEADER_KEYS.contains(headerName)) {
			return true;
		}
		return headerName.contains("token")
				|| headerName.contains("secret")
				|| headerName.contains("password")
				|| headerName.contains("otp")
				|| headerName.contains("api-key");
	}

	protected String rateLimitKey(String endpoint, String subject) {
		String safeSubject = StringUtils.defaultIfBlank(subject, "unknown");
		return "RL:" + endpoint + ":" + safeSubject;
	}

	protected record RateLimitResult(boolean allowed, int limit, long remaining, long retryAfterSeconds) {
	}
}
