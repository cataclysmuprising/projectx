package com.github.projectx.backend.config.security.web;

import com.github.projectx.backend.BackendApplication;
import com.github.projectx.persistence.criteria.rba.AdministratorCriteria;
import com.github.projectx.persistence.criteria.rba.RoleCriteria;
import com.github.projectx.persistence.dto.rba.AdministratorDTO;
import com.github.projectx.persistence.dto.rba.RoleDTO;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.service.rba.AdministratorService;
import jakarta.annotation.Nonnull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

import static com.github.projectx.persistence.utils.LoggerConstants.*;

@Component
public class AuthenticationUserService implements UserDetailsService {
	private static final Logger applicationLogger = LogManager.getLogger("applicationLogs." + AuthenticationUserService.class.getName());
	private static final Logger errorLogger = LogManager.getLogger("errorLogs." + AuthenticationUserService.class.getName());

	@Autowired
	private AdministratorService administratorService;

	@Override
	@Nonnull
	public final UserDetails loadUserByUsername(@Nonnull String loginId) throws UsernameNotFoundException {
		applicationLogger.info(LOG_BREAKER_OPEN);
		try {
			AdministratorCriteria adminCriteria = new AdministratorCriteria();
			adminCriteria.setLoginId(loginId);
			RoleCriteria roleCriteria = new RoleCriteria();
			roleCriteria.setAppName(BackendApplication.APP_NAME);
			adminCriteria.setRole(roleCriteria);
			AdministratorDTO authDTO = administratorService.findOne(adminCriteria, "Administrator(administratorRoles(role))").orElseThrow(() -> new UsernameNotFoundException("Login administrator doesn`t exist !"));
			applicationLogger.info(LOG_PREFIX + "Roles of :{} are {}" + LOG_SUFFIX, authDTO.getName(), authDTO.getRoles());
			Set<String> roleNames = authDTO.getRoles().stream().map(RoleDTO::getName).collect(Collectors.toSet());

			AuthenticatedClient loggedUser = new AuthenticatedClient(authDTO, roleNames);
			applicationLogger.info(LOG_BREAKER_CLOSE);
			return loggedUser;
		}
		catch (PersistenceException e) {
			throw new UsernameNotFoundException("Failed to fetch login administrator informations for given LoginID = <" + loginId + "> !");
		}
	}
}
