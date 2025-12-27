package com.tamantaw.projectx.backend.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class BaseAspect {

	protected static final String LINE =
			"====================================================================";

	protected Method resolveMethod(JoinPoint jp) {
		return ((MethodSignature) jp.getSignature()).getMethod();
	}

	protected Class<?> resolveTargetClass(JoinPoint jp) {
		return jp.getTarget().getClass();
	}

	protected boolean isMvcController(Class<?> clazz) {
		return clazz.isAnnotationPresent(Controller.class)
				|| clazz.isAnnotationPresent(RestController.class);
	}

	protected boolean isProfileAllowed(String profile) {
		if (profile == null || profile.isBlank()) {
			return true;
		}
		String active =
				System.getProperty("spring.profiles.active", "");
		return Arrays.asList(active.split(",")).contains(profile);
	}

	protected List<Object> filterLoggableArgs(Object[] args) {
		if (args == null || args.length == 0) {
			return List.of();
		}

		return Arrays.stream(args)
				.filter(Objects::nonNull)
				.filter(arg ->
						!(arg instanceof HttpServletRequest)
								&& !(arg instanceof HttpServletResponse)
								&& !(arg instanceof BindingResult)
				)
				.toList();
	}

	protected String safeToString(Object obj) {
		try {
			return String.valueOf(obj);
		}
		catch (Exception e) {
			return "[unprintable object]";
		}
	}

	protected long elapsedMs(long startNano) {
		return (System.nanoTime() - startNano) / 1_000_000;
	}
}
