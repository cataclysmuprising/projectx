package com.tamantaw.projectx.persistence.dto;

import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorRoleDTO extends AbstractDTO {
	private AdministratorDTO administrator;
	private RoleDTO role;
}
