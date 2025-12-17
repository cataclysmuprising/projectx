package com.tamantaw.projectx.persistence.exception;

import java.io.Serial;

public class ConsistencyViolationException extends Exception {

	@Serial
	private static final long serialVersionUID = 1875345929841403517L;

	public ConsistencyViolationException() {
		super();
	}

	public ConsistencyViolationException(String message) {
		super(message);
	}

	public ConsistencyViolationException(String message, Throwable cause) {
		super(message, cause);
	}

	public ConsistencyViolationException(Throwable cause) {
		super(cause);
	}
}
