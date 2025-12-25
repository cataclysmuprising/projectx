package com.tamantaw.projectx.persistence.dto;

import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorDTO extends AbstractDTO {
	private String name;

	private String loginId;

	private String password;

	private Administrator.Status status;

	private List<RoleDTO> roles;

	private Set<Long> roleIds;

	public Set<Long> getRoleIds() {
		if (roleIds != null) {
			return roleIds;
		}
		else if (!CollectionUtils.isEmpty(roles)) {
			return roles.stream().map(RoleDTO::getId).collect(Collectors.toSet());
		}
		return null;
	}
}
