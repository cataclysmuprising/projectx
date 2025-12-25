package com.tamantaw.projectx.backend.validators;

import com.tamantaw.projectx.backend.common.response.PageMode;
import com.tamantaw.projectx.backend.common.validation.BaseValidator;
import com.tamantaw.projectx.backend.common.validation.FieldValidator;
import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.AdministratorService;
import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AdmministratorValidator extends BaseValidator {

	@Autowired
	private AdministratorService administratorService;

	@Override
	public boolean supports(@Nonnull Class clazz) {
		return AdministratorDTO.class.equals(clazz);
	}

	@Override
	public void validate(@Nonnull Object obj, @Nonnull Errors errors) {
		AdministratorDTO administratorDTO = (AdministratorDTO) obj;

		// Name
		validateIsEmpty(new FieldValidator("name", "Administrator Name", administratorDTO.getName(), errors));
		validateIsValidMaxValue(new FieldValidator("name", "Administrator Name", administratorDTO.getName(), errors), 100);

		//Password
		validateIsEmpty(new FieldValidator("password", "Password", administratorDTO.getPassword(), errors));
		validateIsValidMinValue(new FieldValidator("password", "Password", administratorDTO.getPassword(), errors), 8);
		validateIsValidMaxValue(new FieldValidator("password", "Password", administratorDTO.getPassword(), errors), 200);

		// Status
		validateIsEmpty(new FieldValidator("status", "Status", administratorDTO.getStatus(), errors));

		// Roles
		validateIsEmpty(new FieldValidator("roleIds", "Role", administratorDTO.getRoleIds(), errors));

		if (pageMode == PageMode.CREATE) {
			//Login ID
			validateIsEmpty(new FieldValidator("loginId", "Login ID", administratorDTO.getLoginId(), errors));
			validateIsValidMaxValue(new FieldValidator("loginId", "Login ID", administratorDTO.getLoginId(), errors), 16);

			if (errors.getFieldErrors("loginId").isEmpty()) {

				AdministratorCriteria criteria = new AdministratorCriteria();
				criteria.setLoginId(administratorDTO.getLoginId());
				try {
					if (administratorService.exists(criteria)) {
						errors.rejectValue("loginId", "", messageSource.getMessage("Validation.common.Field.Unique", "Login ID", administratorDTO.getLoginId()));
					}
				}
				catch (PersistenceException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}
}
