package com.tamantaw.projectx.backend.common.aspect;

import com.tamantaw.projectx.backend.common.annotation.MVCLoggable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
@Component
@Order(0)
public class MVCLoggingAspect {

	private static final Logger log =
			LogManager.getLogger("applicationLogs.mvc");

	private static final String LINE =
			"====================================================================";

	@Around(
			"@within(com.tamantaw.projectx.backend.common.annotation.MVCLoggable) " +
					"|| @annotation(com.tamantaw.projectx.backend.common.annotation.MVCLoggable)"
	)
	public Object logController(ProceedingJoinPoint pjp) throws Throwable {

		MVCLoggable mvcLoggable = resolveLoggable(pjp);

		// ---- ultra-safe guard
		if (mvcLoggable == null) {
			return pjp.proceed();
		}

		// ---- profile guard
		if (!isProfileAllowed(mvcLoggable)) {
			return pjp.proceed();
		}

		Method method = ((MethodSignature) pjp.getSignature()).getMethod();

		if (!isRequestHandlerMethod(method)) {
			return pjp.proceed();
		}

		Class<?> controllerClass = pjp.getTarget().getClass();

		RequestMapping classMapping =
				controllerClass.getAnnotation(RequestMapping.class);

		if (classMapping == null) {
			return pjp.proceed();
		}

		String httpMethod = resolveHttpMethod(method);
		String path = resolvePath(classMapping, method);

		long start = System.nanoTime();

		log.info(LINE);
		log.info("➡ HTTP {} {}", httpMethod, path);
		log.info("↳ Controller: {}#{}",
				controllerClass.getSimpleName(),
				method.getName());

		if (mvcLoggable.logRequest()) {
			logArguments(pjp.getArgs());
		}

		try {
			Object result = pjp.proceed();

			long timeMs = (System.nanoTime() - start) / 1_000_000;

			if (mvcLoggable.logResponse() && result != null) {
				log.debug("⬅ Response: {}", safeToString(result));
			}

			log.info("✔ Completed in {} ms", timeMs);
			log.info(LINE);

			return result;
		}
		catch (Throwable ex) {
			long timeMs = (System.nanoTime() - start) / 1_000_000;

			log.error("✖ Failed after {} ms : {}",
					timeMs,
					ex.getMessage(),
					ex);

			log.info(LINE);
			throw ex;
		}
	}

	// ----------------------------------------------------------------------
	// Helper methods
	// ----------------------------------------------------------------------

	private boolean isProfileAllowed(MVCLoggable loggable) {
		if (loggable == null || loggable.profile().isBlank()) {
			return true;
		}
		return Arrays.asList(
				System.getProperty("spring.profiles.active", "")
						.split(",")
		).contains(loggable.profile());
	}

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

			log.debug("↳ Arg: {}", safeToString(arg));
		}
	}

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

	private MVCLoggable resolveLoggable(ProceedingJoinPoint pjp) {
		Method method = ((MethodSignature) pjp.getSignature()).getMethod();
		Class<?> targetClass = pjp.getTarget().getClass();

		// 1️⃣ Method-level annotation wins
		MVCLoggable loggable = method.getAnnotation(MVCLoggable.class);
		if (loggable != null) {
			return loggable;
		}

		// 2️⃣ Fallback to class-level
		return targetClass.getAnnotation(MVCLoggable.class);
	}

	private boolean isRequestHandlerMethod(Method method) {
		return method.isAnnotationPresent(RequestMapping.class)
				|| method.isAnnotationPresent(GetMapping.class)
				|| method.isAnnotationPresent(PostMapping.class)
				|| method.isAnnotationPresent(PutMapping.class)
				|| method.isAnnotationPresent(DeleteMapping.class);
	}

	private String resolvePath(RequestMapping classMapping, Method method) {

		String base = classMapping.value().length > 0
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

		return base; // no method mapping
	}

	private String safeToString(Object obj) {
		try {
			return String.valueOf(obj);
		}
		catch (Exception e) {
			return "[unprintable object]";
		}
	}

	private String joinPath(String base, String sub) {

		if (base == null) {
			base = "";
		}
		if (sub == null) {
			sub = "";
		}

		// remove trailing slash from base
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}

		// ensure leading slash on sub
		if (!sub.isEmpty() && !sub.startsWith("/")) {
			sub = "/" + sub;
		}

		return base + sub;
	}
}
