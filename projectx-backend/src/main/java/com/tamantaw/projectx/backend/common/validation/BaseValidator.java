package com.tamantaw.projectx.backend.common.validation;

import com.tamantaw.projectx.backend.common.converters.LocalizedMessageResolver;
import com.tamantaw.projectx.backend.common.exception.UnSupportedValidationCheckException;
import com.tamantaw.projectx.backend.common.response.PageMode;
import jakarta.annotation.Nonnull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class BaseValidator implements Validator {

	private static final Pattern DIGITS = Pattern.compile("[\\-+]?\\d+");
	private static final Pattern UNSIGNED_DIGITS = Pattern.compile("\\d+");
	private static final Pattern ALPHABET = Pattern.compile("^[a-zA-Z_ ]+$");

	/* ============================================================
	 * Common helpers
	 * ============================================================ */
	private static final Pattern ALPHANUMERIC = Pattern.compile("^[a-zA-Z_0-9 ]+$");
	private static final Pattern QUERY = Pattern.compile("^[a-zA-Z_0-9 /\\-.@]+$");
	private static final Pattern CAPITAL = Pattern.compile("^[A-Z_ \\-]+$");

	/* ============================================================
	 * Equality
	 * ============================================================ */
	private static final Pattern SMALL = Pattern.compile("^[a-z_ \\-]+$");

	/* ============================================================
	 * Numeric / length validation
	 * ============================================================ */
	private static final Pattern UNICODE = Pattern.compile("^[\\p{L} .'-]+$");
	private static final Pattern EMAIL =
			Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
					Pattern.CASE_INSENSITIVE);

	/* ============================================================
	 * Regex patterns (precompiled)
	 * ============================================================ */
	private final LocalizedMessageResolver messageSource;
	@Getter
	@Setter
	protected PageMode pageMode;

	public BaseValidator(LocalizedMessageResolver messageSource) {
		this.messageSource = messageSource;
	}

	protected void reject(FieldValidator fv, String messageKey, Object... args) {
		fv.getErrors().rejectValue(
				fv.getTargetId(),
				"",
				messageSource.getMessage(messageKey, args)
		);
	}

	protected boolean isEmpty(Object target) {
		switch (target) {
			case null -> {
				return true;
			}
			case String s -> {
				return s.isEmpty();
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

	public boolean validateIsEmpty(FieldValidator fv) {
		if (!fv.getErrors().getFieldErrors(fv.getTargetId()).isEmpty()) {
			return true;
		}

		if (isEmpty(fv.getTarget())) {
			String key = (fv.getTarget() instanceof String || fv.getTarget() == null)
					? "Validation.common.Field.Required"
					: "Validation.common.Field.ChooseOne";

			reject(fv, key, fv.getDisplayName());
			return true;
		}
		return false;
	}

	public void validateIsEqual(String targetId,
	                            FieldValidator fv1,
	                            FieldValidator fv2,
	                            Errors errors) {

		if (!validateIsEmpty(fv1) && !validateIsEmpty(fv2)
				&& !fv1.getTarget().equals(fv2.getTarget())) {

			errors.rejectValue(
					targetId,
					"",
					messageSource.getMessage(
							"Validation.common.Field.DoNotMatch",
							fv1.getDisplayName(),
							fv2.getDisplayName()
					)
			);
		}
	}

	public void validateIsValidMinValue(FieldValidator fv, Number min) {
		if (validateIsEmpty(fv)) {
			return;
		}

		Object target = fv.getTarget();
		if (target instanceof String s && s.length() < min.intValue()) {
			reject(fv, "Validation.common.Field.Min.String", fv.getDisplayName(), min);
		}
		else if (target instanceof Number n && n.doubleValue() < min.doubleValue()) {
			reject(fv, "Validation.common.Field.Min.Number", fv.getDisplayName(), min);
		}
	}

	public void validateIsValidMaxValue(FieldValidator fv, Number max) {
		if (validateIsEmpty(fv)) {
			return;
		}

		Object target = fv.getTarget();
		if (target instanceof String s && s.length() > max.intValue()) {
			reject(fv, "Validation.common.Field.Max.String", fv.getDisplayName(), max);
		}
		else if (target instanceof Number n && n.doubleValue() > max.doubleValue()) {
			reject(fv, "Validation.common.Field.Max.Number", fv.getDisplayName(), max);
		}
		else if (!(target instanceof String || target instanceof Number)) {
			throw new UnSupportedValidationCheckException();
		}
	}

	/* ============================================================
	 * Regex validators
	 * ============================================================ */

	public void validatePattern(FieldValidator fv, Pattern pattern, String messageKey) {
		if (validateIsEmpty(fv)) {
			return;
		}

		if (!(fv.getTarget() instanceof String s) || !pattern.matcher(s).matches()) {
			reject(fv, messageKey, fv.getDisplayName());
		}
	}

	public void validateIsValidEmail(FieldValidator fv) {
		validatePattern(fv, EMAIL, "Validation.common.Field.InvalidEmail");
	}

	public boolean validateIsValidEmail(String value) {
		return value != null && EMAIL.matcher(value).matches();
	}

	/* ============================================================
	 * URL
	 * ============================================================ */

	public void validateIsValidURL(FieldValidator fv) {
		if (validateIsEmpty(fv)) {
			return;
		}

		if (fv.getTarget() instanceof String s) {
			try {
				new URI(s).toURL();
			}
			catch (Exception e) {
				reject(fv, "Validation.common.Field.InvalidURL", s);
			}
		}
		else {
			throw new UnSupportedValidationCheckException();
		}
	}

	/* ============================================================
	 * Validator interface
	 * ============================================================ */

	@Override
	public boolean supports(@Nonnull Class<?> clazz) {
		return true;
	}

	@Override
	public void validate(@Nonnull Object target, @Nonnull Errors errors) {
		// intentionally empty
	}
}

