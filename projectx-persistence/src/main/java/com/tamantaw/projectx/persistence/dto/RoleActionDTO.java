package com.tamantaw.projectx.persistence.dto;

import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class RoleActionDTO extends AbstractDTO {

	private Long roleId;
	private Long actionId;

	private RoleDTO role;

	private ActionDTO action;
}
