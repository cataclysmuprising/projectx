package com.github.projectx.backend.config.security.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthenticationAuditListener {

	private final AuthenticationAuditService authenticationAuditService;

	public AuthenticationAuditListener(AuthenticationAuditService authenticationAuditService) {
		this.authenticationAuditService = authenticationAuditService;
	}

	/**
	 * Normal username/password login
	 */
	@EventListener
	public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
		Authentication authentication = event.getAuthentication();
		ServletRequestAttributes attrs =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

		HttpServletRequest request =
				(attrs != null) ? attrs.getRequest() : null;

		authenticationAuditService.recordSuccess(authentication, request);
	}

	/**
	 * Remember-me login (after restart / cookie-based)
	 */
	@EventListener
	public void onInteractiveAuthenticationSuccess(
			InteractiveAuthenticationSuccessEvent event
	) {
		Authentication authentication = event.getAuthentication();

		ServletRequestAttributes attrs =
				(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

		HttpServletRequest request =
				(attrs != null) ? attrs.getRequest() : null;

		authenticationAuditService.recordSuccess(authentication, request);
	}

//	@EventListener
//	public void onLogout(LogoutSuccessEvent event) {
//		auditService.recordLogout(event.getAuthentication());
//	}

//	@EventListener
//	public void onAuthFailure(AbstractAuthenticationFailureEvent event) {
//		auditService.recordFailure(
//				event.getAuthentication(),
//				event.getException()
//		);
//	}
}

