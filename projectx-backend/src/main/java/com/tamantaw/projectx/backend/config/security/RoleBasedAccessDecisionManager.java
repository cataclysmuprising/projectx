package com.tamantaw.projectx.backend.config.security;

import com.tamantaw.projectx.backend.dto.ActionDefinition;
import com.tamantaw.projectx.backend.utils.ActionRegistry;
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
			LogManager.getLogger(
					"applicationLogs." + RoleBasedAccessDecisionManager.class.getName()
			);

	private final ActionRegistry actionRegistry;

	public RoleBasedAccessDecisionManager(ActionRegistry actionRegistry) {
		this.actionRegistry = actionRegistry;
	}

	@Override
	public void verify(
			@Nullable Supplier<? extends @Nullable Authentication> authentication,
			RequestAuthorizationContext context
	) {
		// Keep override for Spring Security compatibility
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
		applicationLogger.debug("Authentication : {}", auth);

		if (auth == null
				|| !auth.isAuthenticated()
				|| auth.getPrincipal() == null
				|| "anonymousUser".equals(auth.getPrincipal())) {

			applicationLogger.debug(
					"Invalid authentication : need to reauthenticate for current user."
			);
			return new AuthorizationDecision(false);
		}

		String requestURL = context.getRequest().getServletPath();
		applicationLogger.debug(
				"Filtering process executed by RoleBasedAccessDecisionManager for requested URL ==> {}",
				requestURL
		);

		// ---------------------------------------------------------------------
		// Resolve ACTION (Java-side path matching, not DB regex)
		// ---------------------------------------------------------------------
		ActionDefinition action;
		try {
			action = actionRegistry.resolve(requestURL);
		}
		catch (Exception e) {
			applicationLogger.error(
					"Failed to resolve action definition for URL ==> {}",
					requestURL,
					e
			);
			// Safer default: deny if registry resolution fails
			return new AuthorizationDecision(false);
		}

		if (action == null) {
			applicationLogger.debug(
					"No action definition matched for URL ==> {}. Allowing (authenticated already).",
					requestURL
			);
			return new AuthorizationDecision(true);
		}

		applicationLogger.debug(
				"Resolved action for URL ==> {} : [actionId={}, actionName={}]",
				requestURL,
				action.id(),
				action.actionName()
		);

		// ---------------------------------------------------------------------
		// Load allowed roles for resolved ACTION (FROM REGISTRY – NO DB)
		// ---------------------------------------------------------------------
		Set<String> actionAssociatedRoles = action.allowedRoleNames();

		if (actionAssociatedRoles == null || actionAssociatedRoles.isEmpty()) {
			applicationLogger.debug(
					"No role restrictions defined for actionId={} (URL ==> {}). Allowing.",
					action.id(),
					requestURL
			);
			return new AuthorizationDecision(true);
		}

		applicationLogger.debug(
				"URL ==> {} was requested for ActionId={} with allowed Roles => {}",
				requestURL,
				action.id(),
				actionAssociatedRoles
		);

		// ---------------------------------------------------------------------
		// Compare against authenticated user's authorities
		// ---------------------------------------------------------------------
		List<String> authorities = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.toList();

		applicationLogger.debug(
				"Current Authenticated User has owned ==> {}",
				authorities
		);

		boolean hasAuthority =
				CollectionUtils.containsAny(actionAssociatedRoles, authorities);

		if (hasAuthority) {
			applicationLogger.debug(
					"Access Granted : Filtered by RoleBasedAccessDecisionManager."
			);
			return new AuthorizationDecision(true);
		}

		applicationLogger.debug(
				"Access Denied : Filtered by RoleBasedAccessDecisionManager."
		);
		return new AuthorizationDecision(false);
	}
}
