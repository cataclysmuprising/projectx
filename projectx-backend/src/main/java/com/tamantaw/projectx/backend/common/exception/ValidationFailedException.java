package com.tamantaw.projectx.backend.common.exception;

import com.tamantaw.projectx.backend.common.response.PageMessage;
import lombok.Getter;

import java.io.Serial;
import java.util.Map;

@Getter
public class ValidationFailedException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -5158596119154938945L;

	private final Map<String, String> validationErrors;
	private final PageMessage pageMessage;
	private final String errorView;

	public ValidationFailedException(
			Map<String, String> validationErrors,
			PageMessage pageMessage,
			String errorView
	) {
		super("Validation failed");
		this.validationErrors = validationErrors;
		this.pageMessage = pageMessage;
		this.errorView = errorView;
	}
}



