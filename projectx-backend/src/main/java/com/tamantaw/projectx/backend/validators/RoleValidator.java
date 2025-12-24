package com.tamantaw.projectx.backend.validators;

import com.tamantaw.projectx.backend.common.validation.BaseValidator;
import com.tamantaw.projectx.backend.common.validation.FieldValidator;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.service.RoleService;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
public class RoleValidator extends BaseValidator {

	@Autowired
	private RoleService roleService;

	@Override
	public boolean supports(@Nonnull Class clazz) {
		return RoleDTO.class.equals(clazz);
	}

	@Override
	public void validate(@Nonnull Object obj, @Nonnull Errors errors) {
		RoleDTO role = (RoleDTO) obj;
		//Name
		validateIsEmpty(new FieldValidator("name", "Name", role.getName(), errors));
		validateIsValidMaxValue(new FieldValidator("name", "Name", role.getName(), errors), 50);
		//Description
		validateIsEmpty(new FieldValidator("description", "Description", role.getDescription(), errors));
		validateIsValidMaxValue(new FieldValidator("description", "Description", role.getDescription(), errors), 200);
	}
}
