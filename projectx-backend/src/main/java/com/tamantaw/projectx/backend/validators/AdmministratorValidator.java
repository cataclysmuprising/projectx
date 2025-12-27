package com.tamantaw.projectx.backend.validators;

import com.tamantaw.projectx.backend.common.response.PageMode;
import com.tamantaw.projectx.backend.common.validation.BaseValidator;
import com.tamantaw.projectx.backend.common.validation.FieldValidator;
import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.AdministratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AdmministratorValidator extends BaseValidator<AdministratorDTO> {

	@Autowired
	private AdministratorService administratorService;

	@Override
	protected Class<AdministratorDTO> getSupportedClass() {
		return AdministratorDTO.class;
	}

	@Override
	protected void validateTyped(
			AdministratorDTO dto,
			Errors errors,
			PageMode pageMode
	) {

		validateRequired(new FieldValidator("name", "Administrator Name", dto.getName(), errors));
		validateMax(new FieldValidator("name", "Administrator Name", dto.getName(), errors), 100);

		validateRequired(new FieldValidator("password", "Password", dto.getPassword(), errors));
		validateMin(new FieldValidator("password", "Password", dto.getPassword(), errors), 8);
		validateMax(new FieldValidator("password", "Password", dto.getPassword(), errors), 200);

		validateRequired(new FieldValidator("status", "Status", dto.getStatus(), errors));
		validateRequired(new FieldValidator("roleIds", "Role", dto.getRoleIds(), errors));

		if (pageMode == PageMode.CREATE) {

			FieldValidator loginId =
					new FieldValidator("loginId", "Login ID", dto.getLoginId(), errors);

			validateRequired(loginId);
			validateMax(loginId, 16);

			if (!hasErrors(errors, "loginId")) {
				AdministratorCriteria c = new AdministratorCriteria();
				c.setLoginId(dto.getLoginId());

				try {
					if (administratorService.exists(c)) {
						errors.rejectValue(
								"loginId",
								"",
								messageSource.getMessage(
										"Validation.common.Field.Unique",
										"Login ID",
										dto.getLoginId()
								)
						);
					}
				}
				catch (PersistenceException e) {
					errors.reject(
							"Validation.common.SystemError",
							messageSource.getMessage(
									"Validation.common.SystemError",
									null
							)
					);
				}
			}
		}
	}
}
