package com.github.projectx.persistence.exception;

import java.io.Serial;

public class BusinessException extends Exception {

	@Serial
	private static final long serialVersionUID = -3878285173464975547L;

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public BusinessException() {
		super();
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public BusinessException(String message) {
		super(message);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public BusinessException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public BusinessException(Throwable cause) {
		super(cause);
	}
}

