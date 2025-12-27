package com.tamantaw.projectx.backend.common.exceptionHandlers;

import com.tamantaw.projectx.backend.common.exception.RequestValidationException;
import com.tamantaw.projectx.persistence.exception.BusinessException;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.ContentNotFoundException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import jakarta.validation.ConstraintViolationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice(basePackages = "com.tamantaw.projectx.backend.controller.rest")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RestExceptionHandler {

	private static final Logger errorLogger =
			LogManager.getLogger("errorLogs." + RestExceptionHandler.class.getName());

	/* ============================================================
	 * 400 – BAD REQUEST
	 * ============================================================ */

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {

		errorLogger.warn("Validation failed: {}", ex.getMessage());

		ProblemDetail pd = problem(
				HttpStatus.BAD_REQUEST,
				"Validation failed",
				"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.1"
		);

		pd.setProperty("violations", ex.getConstraintViolations());
		return pd;
	}

	@ExceptionHandler({
			UnsatisfiedServletRequestParameterException.class,
			MissingServletRequestParameterException.class
	})
	public ProblemDetail handleBadRequest(Exception ex) {

		errorLogger.warn("Bad request: {}", ex.getMessage());

		return problem(
				HttpStatus.BAD_REQUEST,
				ex.getMessage(),
				"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.1"
		);
	}

	@ExceptionHandler(RequestValidationException.class)
	public ProblemDetail handleRequestValidation(RequestValidationException ex) {

		errorLogger.warn("Validation failed: {}", ex.getMessage());

		ProblemDetail pd = problem(
				HttpStatus.BAD_REQUEST,
				ex.getMessage(),
				"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.1"
		);

		if (ex.getValidationErrors() != null && !ex.getValidationErrors().isEmpty()) {
			pd.setProperty("violations", ex.getValidationErrors());
		}

		return pd;
	}

	/* ============================================================
	 * 401 – UNAUTHORIZED
	 * ============================================================ */

	@ExceptionHandler(SecurityException.class)
	public ProblemDetail handleUnauthorized(SecurityException ex) {

		errorLogger.warn("Unauthorized access: {}", ex.getMessage());

		return problem(
				HttpStatus.UNAUTHORIZED,
				ex.getMessage(),
				"https://datatracker.ietf.org/doc/html/rfc7235#section-3.1"
		);
	}

	/* ============================================================
	 * 403 – FORBIDDEN
	 * ============================================================ */

	@ExceptionHandler({
			AccessDeniedException.class,
			AuthenticationException.class
	})
	public ProblemDetail handleForbidden(Exception ex) {

		errorLogger.warn("Forbidden access: {}", ex.getMessage());

		return problem(
				HttpStatus.FORBIDDEN,
				ex.getMessage(),
				"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.3"
		);
	}

	/* ============================================================
	 * 404 – NOT FOUND
	 * ============================================================ */

	@ExceptionHandler(ContentNotFoundException.class)
	public ProblemDetail handleNotFound(Exception ex) {

		errorLogger.warn("Resource not found: {}", ex.getMessage());

		return problem(
				HttpStatus.NOT_FOUND,
				ex.getMessage(),
				"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.4"
		);
	}

	/* ============================================================
	 * 409 – CONFLICT
	 * ============================================================ */

	@ExceptionHandler(ConsistencyViolationException.class)
	public ProblemDetail handleConflict(ConsistencyViolationException ex) {

		errorLogger.warn("Conflict detected: {}", ex.getMessage());

		return problem(
				HttpStatus.CONFLICT,
				ex.getMessage(),
				"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.8"
		);
	}

	/* ============================================================
	 * 500 – INTERNAL SERVER ERROR
	 * ============================================================ */

	@ExceptionHandler({
			BusinessException.class,
			PersistenceException.class,
			RuntimeException.class
	})
	public ProblemDetail handleServerError(Exception ex) {

		errorLogger.error("Server error occurred", ex);

		ProblemDetail pd = problem(
				HttpStatus.INTERNAL_SERVER_ERROR,
				"Internal server error",
				"https://datatracker.ietf.org/doc/html/rfc7231#section-6.6.1"
		);

		pd.setProperty("errorCategory", "Generic");
		return pd;
	}

	/* ============================================================
	 * Helper
	 * ============================================================ */

	private ProblemDetail problem(HttpStatus status,
	                              String detail,
	                              String typeUri) {

		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setTitle(status.getReasonPhrase());
		pd.setType(URI.create(typeUri));
		pd.setProperty("timestamp", Instant.now());
		return pd;
	}
}
