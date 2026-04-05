package com.github.projectx.persistence.exception;

import java.io.Serial;

public class ContentNotFoundException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -691118741089939098L;

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public ContentNotFoundException() {
		super();
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public ContentNotFoundException(String message) {
		super(message);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public ContentNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public ContentNotFoundException(Throwable cause) {
		super(cause);
	}
}

