package com.github.projectx.backend.common.aspect;

import com.github.projectx.backend.common.annotation.RestLoggable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Aspect
@Component
@Order(0)
public class RestLoggingAspect {

	private static final Logger log =
			LogManager.getLogger("applicationLogs.rest");

	private static final String LINE =
			"====================================================================";
	private static final Pattern PHONE_LIKE = Pattern.compile("^\\+?[0-9]{7,20}$");
	private static final Pattern TOKEN_LIKE = Pattern.compile("^[A-Za-z0-9:_\\-|.=+/]{12,}$");

	private final Environment environment;

	public RestLoggingAspect(Environment environment) {
		this.environment = environment;
	}

	@Around(
			"@within(com.github.projectx.backend.common.annotation.RestLoggable) " +
					"|| @annotation(com.github.projectx.backend.common.annotation.RestLoggable)"
	)
	public Object logRestController(ProceedingJoinPoint pjp) throws Throwable {

		// ------------------------------------------------------------
		// 0. Resolve annotation SAFELY (method > class)
		// ------------------------------------------------------------
		RestLoggable restLoggable = resolveLoggable(pjp);
		if (restLoggable == null) {
			return pjp.proceed();
		}

		Class<?> targetClass = pjp.getTarget().getClass();

		// ------------------------------------------------------------
		// 1. Guard: REST controllers only
		// ------------------------------------------------------------
		if (!targetClass.isAnnotationPresent(RestController.class)) {
			return pjp.proceed();
		}

		// ------------------------------------------------------------
		// 2. Profile guard
		// ------------------------------------------------------------
		if (!isProfileAllowed(restLoggable.profile())) {
			return pjp.proceed();
		}

		Method method = ((MethodSignature) pjp.getSignature()).getMethod();
		String httpMethod = resolveHttpMethod(method);
		String path = resolvePath(targetClass, method);

		long start = System.nanoTime();

		// ------------------------------------------------------------
		// 3. Request logging
		// ------------------------------------------------------------
		log.info(LINE);
		log.info("➡ REST {} {}", httpMethod, path);
		log.info("↳ Handler: {}#{}",
				targetClass.getSimpleName(),
				method.getName()
		);

		if (restLoggable.logRequest()) {
			logArguments(pjp.getArgs());
		}

		try {
			Object result = pjp.proceed();

			long elapsed = elapsedMs(start);

			// ------------------------------------------------------------
			// 4. Response logging
			// ------------------------------------------------------------
			if (restLoggable.logResponse()) {
				logResponse(result);
			}

			log.info("✔ REST completed in {} ms", elapsed);
			log.info(LINE);

			return result;
		}
		catch (Throwable ex) {
			long elapsed = elapsedMs(start);

			log.error(
					"✖ REST failed after {} ms : {}",
					elapsed,
					ex.getMessage(),
					ex
			);

			log.info(LINE);
			throw ex;
		}
	}

	// ----------------------------------------------------------------------
	// Annotation resolution (method > class)
	// ----------------------------------------------------------------------

	private RestLoggable resolveLoggable(ProceedingJoinPoint pjp) {
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();
		Class<?> targetClass = pjp.getTarget().getClass();

		RestLoggable loggable = method.getAnnotation(RestLoggable.class);
		if (loggable != null) {
			return loggable;
		}
		return targetClass.getAnnotation(RestLoggable.class);
	}

	// ----------------------------------------------------------------------
	// Profile guard
	// ----------------------------------------------------------------------

	private boolean isProfileAllowed(String profile) {
		if (profile == null || profile.isBlank()) {
			return true;
		}
		return environment.matchesProfiles(profile);
	}

	// ----------------------------------------------------------------------
	// Argument logging (REST-safe)
	// ----------------------------------------------------------------------

	private void logArguments(Object[] args) {
		if (!log.isDebugEnabled()) {
			return;
		}

		for (Object arg : args) {
			if (arg == null) {
				continue;
			}

			if (arg instanceof HttpServletRequest
					|| arg instanceof HttpServletResponse
					|| arg instanceof BindingResult) {
				continue;
			}

			log.debug("↳ Arg: {}", summarizeForLog(arg));
		}
	}

	// ----------------------------------------------------------------------
	// Response logging
	// ----------------------------------------------------------------------

	private void logResponse(Object response) {
		if (!log.isDebugEnabled() || response == null) {
			return;
		}

		if (response instanceof ResponseEntity<?> re) {
			log.debug("⬅ ActiveState: {}", re.getStatusCode());
			log.debug("⬅ Headers: {}", re.getHeaders().headerSet());

			if (re.getBody() != null) {
				log.debug("⬅ Body: {}", summarizeForLog(re.getBody()));
			}
		}
		else {
			log.debug("⬅ Body: {}", summarizeForLog(response));
		}
	}

	// ----------------------------------------------------------------------
	// Mapping helpers (same logic as MVC)
	// ----------------------------------------------------------------------

	private String resolveHttpMethod(Method method) {
		if (method.isAnnotationPresent(GetMapping.class)) {
			return "GET";
		}
		if (method.isAnnotationPresent(PostMapping.class)) {
			return "POST";
		}
		if (method.isAnnotationPresent(PutMapping.class)) {
			return "PUT";
		}
		if (method.isAnnotationPresent(DeleteMapping.class)) {
			return "DELETE";
		}

		RequestMapping rm = method.getAnnotation(RequestMapping.class);
		if (rm != null && rm.method().length > 0) {
			return rm.method()[0].name();
		}
		return "GET";
	}

	private String resolvePath(Class<?> controller, Method method) {

		RequestMapping classMapping =
				controller.getAnnotation(RequestMapping.class);

		String base = (classMapping != null && classMapping.value().length > 0)
				? classMapping.value()[0]
				: "";

		for (Annotation a : method.getAnnotations()) {
			switch (a) {
				case GetMapping m when m.value().length > 0 -> {
					return joinPath(base, m.value()[0]);
				}
				case PostMapping m when m.value().length > 0 -> {
					return joinPath(base, m.value()[0]);
				}
				case PutMapping m when m.value().length > 0 -> {
					return joinPath(base, m.value()[0]);
				}
				case DeleteMapping m when m.value().length > 0 -> {
					return joinPath(base, m.value()[0]);
				}
				case RequestMapping m when m.value().length > 0 -> {
					return joinPath(base, m.value()[0]);
				}
				default -> {
				}
			}
		}

		// no method-level mapping → class-level only
		return base;
	}

	// ----------------------------------------------------------------------
	// Utilities
	// ----------------------------------------------------------------------

	private long elapsedMs(long startNano) {
		return (System.nanoTime() - startNano) / 1_000_000;
	}

	private String summarizeForLog(Object obj) {
		if (obj == null) {
			return "null";
		}
		if (obj instanceof CharSequence chars) {
			return maskScalar(chars.toString());
		}
		if (obj instanceof Number || obj instanceof Boolean || obj instanceof Enum<?>) {
			return String.valueOf(obj);
		}
		if (obj instanceof HttpServletRequest || obj instanceof HttpServletResponse || obj instanceof BindingResult) {
			return obj.getClass().getSimpleName();
		}
		if (obj instanceof byte[] bytes) {
			return "[bytes length=" + bytes.length + "]";
		}
		if (obj instanceof Object[] array) {
			return "[array length=" + array.length + "]";
		}
		if (obj instanceof Collection<?> collection) {
			return obj.getClass().getSimpleName() + "[size=" + collection.size() + "]";
		}
		if (obj instanceof Map<?, ?> map) {
			return summarizeMap(map);
		}
		return obj.getClass().getSimpleName();
	}

	private String summarizeMap(Map<?, ?> map) {
		List<String> keys = map.keySet()
				.stream()
				.filter(Objects::nonNull)
				.map(String::valueOf)
				.map(this::maskScalar)
				.limit(10)
				.toList();
		return "Map[size=" + map.size() + ", keys=" + keys + "]";
	}

	private String maskScalar(String value) {
		String trimmed = value == null ? "" : value.trim();
		if (trimmed.isEmpty()) {
			return "-";
		}
		if (trimmed.indexOf('@') > 0) {
			return maskEmail(trimmed);
		}
		if (PHONE_LIKE.matcher(trimmed).matches()) {
			return maskPhone(trimmed);
		}
		if (TOKEN_LIKE.matcher(trimmed).matches()) {
			return maskToken(trimmed);
		}
		if (trimmed.length() > 200) {
			return trimmed.substring(0, 200) + "...";
		}
		return trimmed;
	}

	private String maskToken(String value) {
		if (value.length() <= 8) {
			return "***";
		}
		return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
	}

	private String maskPhone(String value) {
		if (value.length() <= 4) {
			return "***";
		}
		return "***" + value.substring(value.length() - 4);
	}

	private String maskEmail(String value) {
		int at = value.indexOf('@');
		if (at <= 1) {
			return "***";
		}
		return value.charAt(0) + "***" + value.substring(at);
	}

	private String joinPath(String base, String sub) {

		if (base == null) {
			base = "";
		}
		if (sub == null) {
			sub = "";
		}

		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		if (!sub.isEmpty() && !sub.startsWith("/")) {
			sub = "/" + sub;
		}
		return base + sub;
	}
}

