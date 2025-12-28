package com.tamantaw.projectx.persistence.dto;

import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import com.tamantaw.projectx.persistence.entity.Role;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString(callSuper = true)
public class RoleDTO extends AbstractDTO {
	private String appName;

	private String name;

	private Role.RoleType roleType;

	private String description;

	private Set<AdministratorDTO> administrators;

	private Set<ActionDTO> actions;

	private Set<Long> actionIds;

	private Set<Long> administratorIds;

	public Set<Long> getActionIds() {
		if (actionIds != null) {
			return actionIds;
		}
		else if (!CollectionUtils.isEmpty(actions)) {
			return actions.stream().map(ActionDTO::getId).collect(Collectors.toSet());
		}
		return null;
	}

	public Set<Long> getAdministratorIds() {
		if (administratorIds != null) {
			return administratorIds;
		}
		else if (!CollectionUtils.isEmpty(administrators)) {
			return administrators.stream().map(AdministratorDTO::getId).collect(Collectors.toSet());
		}
		return null;
	}
}
