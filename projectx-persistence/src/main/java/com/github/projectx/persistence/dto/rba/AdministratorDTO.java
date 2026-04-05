package com.github.projectx.persistence.dto.rba;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.projectx.persistence.dto.base.AbstractDTO;
import com.github.projectx.persistence.entity.rba.Administrator;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jspecify.annotations.Nullable;
import org.springframework.util.CollectionUtils;

import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorDTO extends AbstractDTO {
	private String name;

	private String loginId;

	@JsonIgnore
	private String password;

	private Administrator.Status status;

	private Set<RoleDTO> roles;

	private Set<Long> roleIds;

	/**
	 * Exposes the resolved value through one documented accessor so callers avoid duplicating derivation logic.
	 */
	public @Nullable Set<Long> getRoleIds() {
		if (roleIds != null) {
			return roleIds;
		}
		else if (!CollectionUtils.isEmpty(roles)) {
			return roles.stream().map(RoleDTO::getId).collect(Collectors.toSet());
		}
		return null;
	}
}

