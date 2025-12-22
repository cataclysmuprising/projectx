package com.tamantaw.projectx.backend.config.security;

import com.tamantaw.projectx.persistence.service.AdministratorLoginHistoryService;
import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Component
public class UrlRequestAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
	private static final Logger applicationLogger = LogManager.getLogger("applicationLogs." + UrlRequestAuthenticationSuccessHandler.class.getName());
	@Autowired
	private AdministratorLoginHistoryService administratorLoginHistoryService;

	@Autowired
	private AuthenticationAuditService authenticationAuditService;

	private RequestCache requestCache = new HttpSessionRequestCache();

	public UrlRequestAuthenticationSuccessHandler() {
		super();
		setUseReferer(true);
	}

	@Override
	public void onAuthenticationSuccess(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Authentication authentication) throws ServletException, IOException {
		authenticationAuditService.recordSuccess(authentication, request);
		SavedRequest savedRequest = requestCache.getRequest(request, response);
		if (savedRequest == null) {
			getRedirectStrategy().sendRedirect(request, response, "/");
			return;
		}
		String targetUrlParameter = getTargetUrlParameter();
		if (isAlwaysUseDefaultTargetUrl() || (targetUrlParameter != null && StringUtils.hasText(request.getParameter(targetUrlParameter)))) {
			requestCache.removeRequest(request, response);
			super.onAuthenticationSuccess(request, response, authentication);
			return;
		}
		clearAuthenticationAttributes(request);
		// Use the DefaultSavedRequest URL
		String targetUrl = savedRequest.getRedirectUrl();
		applicationLogger.debug("Redirecting to DefaultSavedRequest Url: {}", targetUrl);
		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}

	@Override
	public void setRequestCache(@Nonnull RequestCache requestCache) {
		this.requestCache = requestCache;
	}
}
