package com.tamantaw.projectx.backend.common.exception;

import java.io.Serial;

public class UnSupportedValidationCheckException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -4209431641032854688L;

	public UnSupportedValidationCheckException() {
		super();
	}

	public UnSupportedValidationCheckException(String message) {
		super(message);
	}

	public UnSupportedValidationCheckException(String message, Throwable cause) {
		super(message, cause);
	}

	public UnSupportedValidationCheckException(Throwable cause) {
		super(cause);
	}
}
