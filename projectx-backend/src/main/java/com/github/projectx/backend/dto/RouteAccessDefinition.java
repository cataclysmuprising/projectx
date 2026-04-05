package com.github.projectx.backend.dto;

import com.github.projectx.persistence.entity.rba.ActionRoute;
import org.springframework.http.server.PathContainer;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Locale;
import java.util.Set;

public record RouteAccessDefinition(
		Long routeId,
		String appName,
		String httpMethod,
		String routePattern,
		ActionRoute.RouteKind routeKind,
		Integer priority,
		PathPattern pathPattern,
		Set<Long> targetActionIds,
		Set<String> targetActionNames,
		Set<String> allowedRoleNames
) {

	public RouteAccessDefinition {
		httpMethod = normalizeMethod(httpMethod);
		priority = (priority != null) ? priority : 0;
		targetActionIds = targetActionIds != null ? Set.copyOf(targetActionIds) : Set.of();
		targetActionNames = targetActionNames != null ? Set.copyOf(targetActionNames) : Set.of();
		allowedRoleNames = allowedRoleNames != null ? Set.copyOf(allowedRoleNames) : Set.of();
	}

	private static String normalizeMethod(String requestMethod) {
		if (requestMethod == null || requestMethod.isBlank()) {
			return "ANY";
		}
		return requestMethod.trim().toUpperCase(Locale.ROOT);
	}

	public boolean matchesExactMethod(String requestMethod, PathContainer requestPath) {
		return !isAnyMethod()
				&& httpMethod.equals(normalizeMethod(requestMethod))
				&& matchesPath(requestPath);
	}

	public boolean matchesAnyMethod(PathContainer requestPath) {
		return isAnyMethod() && matchesPath(requestPath);
	}

	public boolean isAllowedForRoles(Set<String> userRoleNames) {
		return CollectionUtils.containsAny(allowedRoleNames, userRoleNames);
	}

	public boolean isAnyMethod() {
		return "ANY".equals(httpMethod);
	}

	private boolean matchesPath(PathContainer requestPath) {
		return requestPath != null && pathPattern.matches(requestPath);
	}
}
