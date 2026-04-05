package com.github.projectx.persistence.exception;

import java.io.Serial;

public class ConsistencyViolationException extends Exception {

	@Serial
	private static final long serialVersionUID = 1875345929841403517L;

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public ConsistencyViolationException() {
		super();
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public ConsistencyViolationException(String message) {
		super(message);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public ConsistencyViolationException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public ConsistencyViolationException(Throwable cause) {
		super(cause);
	}
}

