package com.github.projectx.persistence.exception;

import com.github.projectx.persistence.dto.response.PageMessage;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ValidationExceptionTranslator {

	private ValidationExceptionTranslator() {
	}

	/* =========================================================
	   Bean Validation (Jakarta)
	   ========================================================= */

	/**
	 * Normalizes bean-validation violations into the module's UI-facing validation shape so callers
	 * can handle all validation failures through one exception type.
	 */
	public static ValidationFailedException fromJakartaViolation(
			ConstraintViolationException e,
			String errorView,
			PageMessage pageMessage
	) {
		Map<String, String> errors = new LinkedHashMap<>();

		for (ConstraintViolation<?> v : e.getConstraintViolations()) {
			String field = v.getPropertyPath().toString();
			errors.put(field, v.getMessage());
		}

		return new ValidationFailedException(
				errors,
				pageMessage,
				errorView
		);
	}

	/* =========================================================
	   Hibernate / DB Constraint (UNIQUE, FK, etc.)
	   ========================================================= */

	/**
	 * Converts low-level database constraint failures into stable user-facing messages so API and
	 * UI layers do not depend on vendor-specific SQL error text.
	 */
	public static ValidationFailedException fromHibernateViolation(
			org.hibernate.exception.ConstraintViolationException e,
			String errorView,
			PageMessage pageMessage
	) {
		Map<String, String> errors = new LinkedHashMap<>();

		SQLException sqlEx = e.getSQLException();

		String message = (sqlEx != null && sqlEx.getMessage() != null)
				? sqlEx.getMessage()
				: "Data integrity violation";

		/*
		 * DB constraint violations are NOT field validation errors.
		 * Treat them as global errors unless caller decides otherwise.
		 */
		errors.put("_global", normalizeDbMessage(message));

		return new ValidationFailedException(
				errors,
				pageMessage,
				errorView
		);
	}

	private static String normalizeDbMessage(String raw) {
		String msg = raw.toLowerCase();

		if (msg.contains("duplicate") || msg.contains("already exists")) {
			return "Duplicate value already exists";
		}

		if (msg.contains("foreign key")) {
			return "Referenced record does not exist";
		}

		if (msg.contains("null value")) {
			return "Required value is missing";
		}

		return "Invalid data";
	}
}

