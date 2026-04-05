package com.github.projectx.backend.config.security.web;

import com.github.projectx.backend.dto.RouteAccessDefinition;
import com.github.projectx.backend.utils.ActionRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

@Component
public class RoleBasedAccessDecisionManager
		implements AuthorizationManager<RequestAuthorizationContext> {

	private static final Logger applicationLogger =
			LogManager.getLogger("applicationLogs." + RoleBasedAccessDecisionManager.class.getName());

	private final ActionRegistry actionRegistry;

	public RoleBasedAccessDecisionManager(ActionRegistry actionRegistry) {
		this.actionRegistry = actionRegistry;
	}

	@Override
	public void verify(
			@Nullable Supplier<? extends @Nullable Authentication> authentication,
			RequestAuthorizationContext context
	) {
		if (authentication != null) {
			AuthorizationManager.super.verify(authentication, context);
		}
	}

	@Override
	public @Nullable AuthorizationResult authorize(
			Supplier<? extends @Nullable Authentication> authentication,
			RequestAuthorizationContext context
	) {
		Authentication auth = authentication.get();
		if (applicationLogger.isDebugEnabled()) {
			applicationLogger.debug(
					"Authentication summary: type={}, name={}, authenticated={}, authorities={}",
					auth == null ? "null" : auth.getClass().getSimpleName(),
					auth == null ? "anonymous" : auth.getName(),
					auth != null && auth.isAuthenticated(),
					auth == null ? List.of() : auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList()
			);
		}

		if (auth == null
				|| !auth.isAuthenticated()
				|| auth.getPrincipal() == null
				|| "anonymousUser".equals(auth.getPrincipal())) {
			applicationLogger.debug("Invalid authentication : need to reauthenticate for current user.");
			return new AuthorizationDecision(false);
		}

		String requestURL = resolveApplicationPath(context.getRequest());
		String requestMethod = context.getRequest().getMethod();

		RouteAccessDefinition routeAccess;
		try {
			routeAccess = actionRegistry.resolve(requestURL, requestMethod);
		}
		catch (Exception e) {
			applicationLogger.error(
					"Failed to resolve route access definition for method={} path={}",
					requestMethod,
					requestURL,
					e
			);
			return new AuthorizationDecision(false);
		}

		if (routeAccess == null) {
			if (isProtectedActionPath(requestURL)) {
				logDenied("unmapped_protected_route", auth, requestMethod, requestURL, null, List.of());
				return new AuthorizationDecision(false);
			}
			applicationLogger.debug(
					"No route access definition matched for method={} path={}. Allowing (authenticated already).",
					requestMethod,
					requestURL
			);
			return new AuthorizationDecision(true);
		}

		Set<String> allowedRoleNames = routeAccess.allowedRoleNames();
		List<String> authorities = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		if (allowedRoleNames == null || allowedRoleNames.isEmpty()) {
			if (isProtectedActionPath(requestURL)) {
				logDenied("protected_route_without_roles", auth, requestMethod, requestURL, routeAccess, authorities);
				return new AuthorizationDecision(false);
			}
			applicationLogger.debug(
					"No role restrictions defined for unprotected routeId={} method={} path={}. Allowing.",
					routeAccess.routeId(),
					requestMethod,
					requestURL
			);
			return new AuthorizationDecision(true);
		}

		boolean hasAuthority = CollectionUtils.containsAny(allowedRoleNames, authorities);
		if (hasAuthority) {
			applicationLogger.debug(
					"Access granted [method={}][path={}][routeId={}][routeKind={}][targets={}]",
					requestMethod,
					requestURL,
					routeAccess.routeId(),
					routeAccess.routeKind(),
					routeAccess.targetActionNames()
			);
			return new AuthorizationDecision(true);
		}

		logDenied("role_mismatch", auth, requestMethod, requestURL, routeAccess, authorities);
		return new AuthorizationDecision(false);
	}

	private void logDenied(
			String reason,
			Authentication auth,
			String requestMethod,
			String requestURL,
			@Nullable RouteAccessDefinition routeAccess,
			List<String> authorities
	) {
		applicationLogger.warn(
				"Access denied [reason={}][user={}][method={}][path={}][authorities={}][routeId={}][routeKind={}][targets={}][allowedRoles={}]",
				reason,
				auth == null ? "anonymous" : auth.getName(),
				requestMethod,
				requestURL,
				authorities,
				routeAccess == null ? null : routeAccess.routeId(),
				routeAccess == null ? null : routeAccess.routeKind(),
				routeAccess == null ? List.of() : routeAccess.targetActionNames(),
				routeAccess == null ? List.of() : routeAccess.allowedRoleNames()
		);
	}

	private boolean isProtectedActionPath(String requestURL) {
		if (requestURL == null) {
			return false;
		}
		return requestURL.startsWith("/api/web/sec/")
				|| requestURL.startsWith("/web/sec/")
				|| requestURL.startsWith("/actuator");
	}

	private String resolveApplicationPath(jakarta.servlet.http.HttpServletRequest request) {
		String requestUri = request == null ? "" : request.getRequestURI();
		String contextPath = request == null ? "" : request.getContextPath();

		if (requestUri == null || requestUri.isBlank()) {
			String servletPath = request == null ? "" : request.getServletPath();
			return servletPath == null ? "" : servletPath;
		}

		if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
			return requestUri.substring(contextPath.length());
		}
		return requestUri;
	}
}
