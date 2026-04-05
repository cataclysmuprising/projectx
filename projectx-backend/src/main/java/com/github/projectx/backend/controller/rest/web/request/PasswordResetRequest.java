package com.github.projectx.backend.controller.rest.web.request;

import org.apache.commons.lang3.StringUtils;

public class PasswordResetRequest {

	private String password;

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "PasswordResetRequest[password=" + maskedPassword(password) + "]";
	}

	private String maskedPassword(String rawPassword) {
		if (StringUtils.isBlank(rawPassword)) {
			return "(blank)";
		}
		return "********";
	}
}
