package com.tamantaw.projectx.backend.config.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RememberMeAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

	@Autowired
	private AuthenticationAuditService authenticationAuditService;

	@Override
	public void onAuthenticationSuccess(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Authentication authentication) throws IOException, ServletException {
		authenticationAuditService.recordSuccess(authentication, request);
		super.setAlwaysUseDefaultTargetUrl(true);
		super.setDefaultTargetUrl(request.getRequestURL().toString());
		super.onAuthenticationSuccess(request, response, authentication);
	}
}
