package com.tamantaw.projectx.backend.common.validation;

import com.tamantaw.projectx.backend.common.converters.LocalizedMessageResolver;
import com.tamantaw.projectx.backend.common.response.PageMode;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.Collection;
import java.util.Map;

public abstract class BaseValidator<T> implements Validator {

	@Autowired
	protected LocalizedMessageResolver messageSource;

	// ------------------------------------------------------------
	// Spring Validator contract
	// ------------------------------------------------------------

	@Override
	public final boolean supports(@Nonnull Class<?> clazz) {
		return getSupportedClass().isAssignableFrom(clazz);
	}

	protected abstract Class<T> getSupportedClass();

	@Override
	@SuppressWarnings("unchecked")
	public final void validate(@Nonnull Object target, @Nonnull Errors errors) {
		validateTyped((T) target, errors, PageMode.VIEW);
	}

	// ------------------------------------------------------------
	// Explicit validation entry
	// ------------------------------------------------------------

	public final void validate(
			Object target,
			Errors errors,
			PageMode pageMode
	) {
		if (!supports(target.getClass())) {
			throw new IllegalArgumentException(
					"Validator " + getClass().getSimpleName()
							+ " does not support " + target.getClass().getSimpleName()
			);
		}
		validateTyped(getSupportedClass().cast(target), errors, pageMode);
	}

	protected abstract void validateTyped(
			T target,
			Errors errors,
			PageMode pageMode
	);

	// ------------------------------------------------------------
	// Common helpers (unchanged, but cleaner)
	// ------------------------------------------------------------

	protected void reject(FieldValidator fv, String key, Object... args) {
		fv.getErrors().rejectValue(
				fv.getTargetId(),
				"",
				messageSource.getMessage(key, args)
		);
	}

	protected boolean hasErrors(Errors errors, String field) {
		return !errors.getFieldErrors(field).isEmpty();
	}

	protected boolean isEmpty(Object target) {
		switch (target) {
			case null -> {
				return true;
			}
			case String s -> {
				return s.isBlank();
			}
			case Collection<?> c -> {
				return c.isEmpty();
			}
			case Map<?, ?> m -> {
				return m.isEmpty();
			}
			default -> {
			}
		}
		if (target.getClass().isArray()) {
			return ((Object[]) target).length == 0;
		}
		return false;
	}

	protected boolean validateRequired(FieldValidator fv) {
		if (hasErrors(fv.getErrors(), fv.getTargetId())) {
			return true;
		}
		if (isEmpty(fv.getTarget())) {
			reject(
					fv,
					fv.getTarget() instanceof String
							? "Validation.common.Field.Required"
							: "Validation.common.Field.ChooseOne",
					fv.getDisplayName()
			);
			return true;
		}
		return false;
	}

	protected void validateMin(FieldValidator fv, Number min) {
		if (validateRequired(fv)) {
			return;
		}

		Object v = fv.getTarget();
		if (v instanceof String s && s.length() < min.intValue()) {
			reject(fv, "Validation.common.Field.Min.String", fv.getDisplayName(), min);
		}
		else if (v instanceof Number n && n.doubleValue() < min.doubleValue()) {
			reject(fv, "Validation.common.Field.Min.Number", fv.getDisplayName(), min);
		}
	}

	protected void validateMax(FieldValidator fv, Number max) {
		if (validateRequired(fv)) {
			return;
		}

		Object v = fv.getTarget();
		if (v instanceof String s && s.length() > max.intValue()) {
			reject(fv, "Validation.common.Field.Max.String", fv.getDisplayName(), max);
		}
		else if (v instanceof Number n && n.doubleValue() > max.doubleValue()) {
			reject(fv, "Validation.common.Field.Max.Number", fv.getDisplayName(), max);
		}
	}
}


