package com.tamantaw.projectx.backend.controller.rest;

import com.tamantaw.projectx.backend.config.security.AuthenticatedClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public abstract class BaseRESTController {

	@Autowired
	protected PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper mapper;

	protected Long getSignInClientId() {
		AuthenticatedClient loginUser = getSignInClientInfo();
		if (loginUser != null) {
			return loginUser.getId();
		}
		return null;
	}

	protected AuthenticatedClient getSignInClientInfo() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getPrincipal() == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
			return null;
		}
		return mapper.convertValue(auth.getPrincipal(), AuthenticatedClient.class);
	}

	protected boolean containsIgnoreCase(List<String> list, String soughtFor) {
		for (String current : list) {
			if (current.equalsIgnoreCase(soughtFor)) {
				return true;
			}
		}
		return false;
	}
}

