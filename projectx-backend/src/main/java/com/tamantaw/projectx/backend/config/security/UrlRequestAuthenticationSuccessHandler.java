package com.tamantaw.projectx.backend.config.security;

import com.tamantaw.projectx.backend.utils.SecurityUtil;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.dto.AdministratorLoginHistoryDTO;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
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
import java.time.LocalDateTime;

import static com.tamantaw.projectx.persistence.utils.LoggerConstants.LOG_PREFIX;
import static com.tamantaw.projectx.persistence.utils.LoggerConstants.LOG_SUFFIX;

@Component
public class UrlRequestAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {
	private static final Logger applicationLogger = LogManager.getLogger("applicationLogs." + UrlRequestAuthenticationSuccessHandler.class.getName());
	private static final Logger errorLogger = LogManager.getLogger("errorLogs." + UrlRequestAuthenticationSuccessHandler.class.getName());
	@Autowired
	private AdministratorLoginHistoryService administratorLoginHistoryService;
	private RequestCache requestCache = new HttpSessionRequestCache();

	public UrlRequestAuthenticationSuccessHandler() {
		super();
		setUseReferer(true);
	}

	@Override
	public void onAuthenticationSuccess(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
		String loginId = authentication.getName();
		applicationLogger.info(LOG_PREFIX + "Login Client with Login ID  '" + loginId + "' has successfully signed in." + LOG_SUFFIX);
		AuthenticatedClient authClient = (AuthenticatedClient) authentication.getPrincipal();
		if (authClient != null && authClient.getUserDetail() != null) {
			try {
				AdministratorDTO administrator = authClient.getUserDetail();
				AdministratorLoginHistoryDTO loginHistory = new AdministratorLoginHistoryDTO();
				loginHistory.setIpAddress(SecurityUtil.getClientIp(request));
				loginHistory.setOs(SecurityUtil.getOperatingSystem(request));
				loginHistory.setClientAgent(SecurityUtil.getUserAgent(request));
				loginHistory.setLoginDate(LocalDateTime.now());
				loginHistory.setAdministrator(administrator);
				administratorLoginHistoryService.create(loginHistory, administrator.getId());
				applicationLogger.info(LOG_PREFIX + "Recorded in loginHistory for Login ID '" + loginId + "'." + LOG_SUFFIX);
			}
			catch (PersistenceException | ConsistencyViolationException e) {
				errorLogger.error(LOG_PREFIX + "Can't save in loginHistory for Login ID '" + loginId + "'" + LOG_SUFFIX, e);
			}
		}
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
		applicationLogger.debug("Redirecting to DefaultSavedRequest Url: " + targetUrl);
		getRedirectStrategy().sendRedirect(request, response, targetUrl);
	}

	@Override
	public void setRequestCache(@Nonnull RequestCache requestCache) {
		this.requestCache = requestCache;
	}
}
