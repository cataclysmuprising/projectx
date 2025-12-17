package com.tamantaw.projectx.persistence.exception;

import java.io.Serial;

public class ContentNotFoundException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = -691118741089939098L;

	public ContentNotFoundException() {
		super();
	}

	public ContentNotFoundException(String message) {
		super(message);
	}

	public ContentNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ContentNotFoundException(Throwable cause) {
		super(cause);
	}
}
