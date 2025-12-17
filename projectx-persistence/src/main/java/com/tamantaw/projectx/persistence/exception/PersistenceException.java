package com.tamantaw.projectx.persistence.exception;

import java.io.Serial;

public class PersistenceException extends Exception {

	@Serial
	private static final long serialVersionUID = -7512756642706562435L;

	public PersistenceException() {
		super();
	}

	public PersistenceException(String message) {
		super(message);
	}

	public PersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	public PersistenceException(Throwable cause) {
		super(cause);
	}
}
