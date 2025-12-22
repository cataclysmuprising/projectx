package com.tamantaw.projectx.backend.config.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

@Component
public class CustomAuthenticationFailureHandler
		extends SimpleUrlAuthenticationFailureHandler {

	private final MessageSource messageSource;

	public CustomAuthenticationFailureHandler(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@Override
	public void onAuthenticationFailure(
			@Nonnull HttpServletRequest request,
			@Nonnull HttpServletResponse response,
			AuthenticationException exception
	) throws IOException, ServletException {

		Locale locale = Locale.getDefault();
		String errorKey = "loginfailed";
		String messageCode = "Serverity.common.auth.message.badCredentials";

		switch (exception) {
			case DisabledException disabledException -> {
				errorKey = "account-disabled";
				messageCode = "Serverity.common.auth.message.disabled";
			}
			case LockedException lockedException -> {
				errorKey = "account-locked";
				messageCode = "Serverity.common.auth.message.locked";
			}
			case AccountExpiredException accountExpiredException -> {
				errorKey = "account-expired";
				messageCode = "Serverity.common.auth.message.expired";
			}
			default -> {
			}
		}

		String errorMessage = messageSource.getMessage(
				messageCode, null, locale
		);

		request.getSession()
				.setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, errorMessage);

		setDefaultFailureUrl("/login?error=" + errorKey);
		super.onAuthenticationFailure(request, response, exception);
	}
}
