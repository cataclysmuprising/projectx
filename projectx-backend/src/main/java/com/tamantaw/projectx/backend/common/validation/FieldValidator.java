package com.tamantaw.projectx.backend.common.validation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.Errors;

@Setter
@Getter
public class FieldValidator {
	private String targetId;
	private String displayName;
	private Object target;
	private Errors errors;

	public FieldValidator(String targetId, String displayName, Object target, Errors errors) {
		this.targetId = targetId;
		this.displayName = displayName;
		this.target = target;
		this.errors = errors;
	}
}
