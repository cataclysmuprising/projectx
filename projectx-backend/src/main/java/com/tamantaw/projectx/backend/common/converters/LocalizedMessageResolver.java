package com.tamantaw.projectx.backend.common.converters;

import jakarta.annotation.Nonnull;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class LocalizedMessageResolver implements MessageSourceAware {

	private MessageSource messageSource;

	@Override
	public void setMessageSource(@Nonnull MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	// message key is default message
	public String getMessage(String code, Object... object) {
		Locale locale = LocaleContextHolder.getLocale();
		String message;
		try {
			message = messageSource.getMessage(code, object, locale);
		}
		catch (Exception e) {
			return code;
		}
		return message;
	}
}
