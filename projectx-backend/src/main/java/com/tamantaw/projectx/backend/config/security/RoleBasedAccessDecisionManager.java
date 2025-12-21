package com.tamantaw.projectx.backend.config.security;

import com.tamantaw.projectx.backend.BackendApplication;
import com.tamantaw.projectx.persistence.service.RoleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RoleBasedAccessDecisionManager implements AuthorizationManager<RequestAuthorizationContext> {
	private static final Logger applicationLogger = LogManager.getLogger("applicationLogs." + RoleBasedAccessDecisionManager.class.getName());
	private final RoleService roleService;

	public RoleBasedAccessDecisionManager(RoleService roleService) {
		this.roleService = roleService;
	}

	private List<String> getAssociatedRolesByUrl(String url) {
		List<String> urlAssociatedRoles = null;
		try {
			urlAssociatedRoles = roleService.selectRolesByActionURL(url, BackendApplication.APP_NAME);
		}
		catch (Exception e) {
			applicationLogger.error("Failed to load associated roles for URL ==> " + url);
		}
		return urlAssociatedRoles;
	}

	@Override
	public void verify(@Nullable Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
		if (authentication != null) {
			AuthorizationManager.super.verify(authentication, context);
		}
	}

	@Override
	public @Nullable AuthorizationResult authorize(Supplier<? extends @Nullable Authentication> authentication, RequestAuthorizationContext context) {
		AuthorizationDecision decision = new AuthorizationDecision(true);
		Authentication auth = authentication.get();
		applicationLogger.debug("Authentication : " + auth);
		if (auth == null || auth.getPrincipal() == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
			applicationLogger.debug("Invalid authentication : need to reauthenticate for current user.");
			return new AuthorizationDecision(false);
		}

		String requestURL = context.getRequest().getServletPath();
		applicationLogger.debug("Filtering process executed by RoleBasedAccessDecisionManager for requested URL ==> {}", requestURL);
		List<String> urlAssociatedRoles = getAssociatedRolesByUrl(requestURL);
		if (urlAssociatedRoles == null || urlAssociatedRoles.isEmpty()) {
			applicationLogger.debug("Access restrictions were not defined for URL ==> {}.", requestURL);
			return null;
		}
		applicationLogger.debug("URL ==> {} was requested for Roles => {} ", requestURL, urlAssociatedRoles);
		List<String> authorities = auth.getAuthorities().parallelStream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
		applicationLogger.debug("Current Authenticated User has owned ==> {}", authorities);
		boolean hasAuthority = CollectionUtils.containsAny(urlAssociatedRoles, authorities);
		if (hasAuthority) {
			applicationLogger.debug("Access Granted : Filtered by RoleBasedAccessDecisionManager.");
			return new AuthorizationDecision(true);
		}
		else {
			applicationLogger.debug("Access Denied : Filtered by RoleBasedAccessDecisionManager.");
			return new AuthorizationDecision(false);
		}
	}
}
