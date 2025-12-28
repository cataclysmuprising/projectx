package com.tamantaw.projectx.persistence.dto;

import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@Getter
@Setter
@ToString(callSuper = true)
public class ActionDTO extends AbstractDTO {
	private String appName;

	private String page;

	private String actionName;

	private String displayName;

	private Action.ActionType actionType;

	private String url;

	private String description;

	private Set<RoleDTO> roles;
}
