package com.github.projectx.persistence.exception;

import java.io.Serial;

public class PersistenceException extends Exception {

	@Serial
	private static final long serialVersionUID = -7512756642706562435L;

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public PersistenceException() {
		super();
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public PersistenceException(String message) {
		super(message);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public PersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public PersistenceException(Throwable cause) {
		super(cause);
	}
}

