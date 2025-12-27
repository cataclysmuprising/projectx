package com.tamantaw.projectx.backend.validators;

import com.tamantaw.projectx.backend.common.response.PageMode;
import com.tamantaw.projectx.backend.common.validation.BaseValidator;
import com.tamantaw.projectx.backend.common.validation.FieldValidator;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RoleValidator extends BaseValidator<RoleDTO> {

	@Autowired
	private RoleService roleService;

	@Override
	protected Class<RoleDTO> getSupportedClass() {
		return RoleDTO.class;
	}

	@Override
	protected void validateTyped(
			RoleDTO role,
			Errors errors,
			PageMode pageMode
	) {

		// ------------------------------------------------------------
		// Name
		// ------------------------------------------------------------
		FieldValidator name =
				new FieldValidator("name", "Name", role.getName(), errors);

		validateRequired(name);
		validateMax(name, 50);

		// ------------------------------------------------------------
		// Description
		// ------------------------------------------------------------
		FieldValidator description =
				new FieldValidator(
						"description",
						"Description",
						role.getDescription(),
						errors
				);

		validateRequired(description);
		validateMax(description, 200);
	}
}
