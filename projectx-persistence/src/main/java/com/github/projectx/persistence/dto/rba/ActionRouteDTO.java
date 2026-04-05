package com.github.projectx.persistence.dto.rba;

import com.github.projectx.persistence.dto.base.AbstractDTO;
import com.github.projectx.persistence.entity.rba.ActionRoute;
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
public class ActionRouteDTO extends AbstractDTO {
	private String appName;
	private String routePattern;
	private String httpMethod;
	private ActionRoute.RouteKind routeKind;
	private Integer priority;
	private Boolean active;
	private String description;
	private Set<ActionDTO> actions;
	private Set<Long> actionIds;

	public @Nullable Set<Long> getActionIds() {
		if (actionIds != null) {
			return actionIds;
		}
		else if (!CollectionUtils.isEmpty(actions)) {
			return actions.stream().map(ActionDTO::getId).collect(Collectors.toSet());
		}
		return null;
	}
}
