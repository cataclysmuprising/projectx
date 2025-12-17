package com.tamantaw.projectx.persistence.exception;

import java.io.Serial;

public class BusinessException extends Exception {

	@Serial
	private static final long serialVersionUID = -3878285173464975547L;

	public BusinessException() {
		super();
	}

	public BusinessException(String message) {
		super(message);
	}

	public BusinessException(String message, Throwable cause) {
		super(message, cause);
	}

	public BusinessException(Throwable cause) {
		super(cause);
	}
}
