package com.github.projectx.persistence.dto.rba;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.projectx.persistence.dto.base.AbstractDTO;
import com.github.projectx.persistence.entity.rba.Action;
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

	private Action.AccessLevel accessLevel;

	@JsonIgnore
	private String url;

	private String description;

	private Set<RoleDTO> roles;

	private String primaryRoute;

	private Long totalRouteCount;

	private Long pageSupportRouteCount;

	private Long sharedLookupRouteCount;
}

