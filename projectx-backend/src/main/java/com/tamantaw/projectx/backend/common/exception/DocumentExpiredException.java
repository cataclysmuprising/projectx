package com.tamantaw.projectx.backend.common.exception;

import java.io.Serial;

public class DocumentExpiredException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 7119088293563147807L;

	public DocumentExpiredException() {
		super();
	}

	public DocumentExpiredException(String message) {
		super(message);
	}

	public DocumentExpiredException(String message, Throwable cause) {
		super(message, cause);
	}

	public DocumentExpiredException(Throwable cause) {
		super(cause);
	}
}
