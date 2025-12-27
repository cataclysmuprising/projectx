package com.tamantaw.projectx.backend.common.exception;

import lombok.Getter;

import java.io.Serial;
import java.util.Map;

@Getter
public class RequestValidationException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -8717980876223546171L;
	private final Map<String, String> validationErrors;

	public RequestValidationException(String message) {
		this(message, null);
	}

	public RequestValidationException(String message, Map<String, String> validationErrors) {
		super(message);
		this.validationErrors = validationErrors;
	}
}
