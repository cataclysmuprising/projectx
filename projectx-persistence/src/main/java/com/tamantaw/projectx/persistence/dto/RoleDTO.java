package com.tamantaw.projectx.persistence.dto;

import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import com.tamantaw.projectx.persistence.entity.Role;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class RoleDTO extends AbstractDTO {
	private String appName;

	private String name;

	private Role.RoleType roleType;

	private String description;

	private List<AdministratorDTO> administrators;

	private List<ActionDTO> actions;
}
