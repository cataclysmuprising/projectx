package com.tamantaw.projectx.backend.config.security;

import com.tamantaw.projectx.backend.utils.SecurityUtil;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.dto.AdministratorLoginHistoryDTO;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.AdministratorLoginHistoryService;
import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.tamantaw.projectx.persistence.utils.LoggerConstants.LOG_PREFIX;
import static com.tamantaw.projectx.persistence.utils.LoggerConstants.LOG_SUFFIX;

@Service
public class AuthenticationAuditService {

	private static final Logger applicationLogger = LogManager.getLogger("applicationLogs." + AuthenticationAuditService.class.getName());
	private static final Logger errorLogger = LogManager.getLogger("errorLogs." + AuthenticationAuditService.class.getName());

	private final AdministratorLoginHistoryService historyService;

	public AuthenticationAuditService(
			AdministratorLoginHistoryService historyService
	) {
		this.historyService = historyService;
	}

	public void recordSuccess(@Nonnull Authentication authentication, HttpServletRequest request) {
		if (!(authentication.getPrincipal() instanceof AuthenticatedClient client)) {
			return;
		}

		AdministratorDTO admin = client.getUserDetail();
		if (admin == null) {
			return;
		}
		applicationLogger.info(LOG_PREFIX + "Login Client with Login ID  '{}' has successfully signed in." + LOG_SUFFIX, authentication.getName());
		AdministratorLoginHistoryDTO history = new AdministratorLoginHistoryDTO();
		history.setAdministrator(admin);
		history.setIpAddress(SecurityUtil.getClientIp(request));
		history.setOs(SecurityUtil.getOperatingSystem(request));
		history.setClientAgent(SecurityUtil.getUserAgent(request));
		history.setLoginDate(LocalDateTime.now());

		try {
			historyService.create(history, admin.getId());
		}
		catch (PersistenceException | ConsistencyViolationException e) {
			errorLogger.error(LOG_PREFIX + "Can't save in loginHistory for Login ID '{}'" + LOG_SUFFIX, authentication.getName(), e);
		}
	}
}

