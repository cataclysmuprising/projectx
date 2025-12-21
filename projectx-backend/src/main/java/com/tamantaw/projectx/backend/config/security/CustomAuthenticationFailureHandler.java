package com.tamantaw.projectx.backend.config.security;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

	@Autowired
	private MessageSource messageSource;

	@Override
	public void onAuthenticationFailure(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
		String causes = "loginfailed";
		Locale locale = Locale.getDefault();
		String errorMessage = messageSource.getMessage("Serverity.common.auth.message.badCredentials", null, locale);
		if (exception.getMessage().equalsIgnoreCase("User is disabled")) {
			causes = "account-disabled";
			errorMessage = messageSource.getMessage("Serverity.common.auth.message.disabled", null, locale);
		}
		else if (exception.getMessage().equalsIgnoreCase("User account is locked")) {
			causes = "account-locked";
			errorMessage = messageSource.getMessage("Serverity.common.auth.message.locked", null, locale);
		}
		else if (exception.getMessage().equalsIgnoreCase("User account has expired")) {
			causes = "account-expired";
			errorMessage = messageSource.getMessage("Serverity.common.auth.message.expired", null, locale);
		}
		setDefaultFailureUrl("/login?error=" + causes);
		super.onAuthenticationFailure(request, response, exception);

		request.getSession().setAttribute(WebAttributes.AUTHENTICATION_EXCEPTION, errorMessage);
	}
}
