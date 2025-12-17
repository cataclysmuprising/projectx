package com.tamantaw.projectx.persistence.dto;

import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorDTO extends AbstractDTO {
	private String name;

	private String loginId;

	private String password;

	private Administrator.Status status;

	private List<RoleDTO> roles;
}
