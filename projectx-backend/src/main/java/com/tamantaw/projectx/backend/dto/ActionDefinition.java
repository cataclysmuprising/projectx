package com.tamantaw.projectx.backend.dto;

import com.tamantaw.projectx.persistence.entity.Action;
import org.springframework.http.server.PathContainer;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Set;

public record ActionDefinition(Long id, String appName, String page, String actionName, String displayName, Action.ActionType actionType, PathPattern pathPattern, Set<String> allowedRoleNames) {

	public ActionDefinition(
			Long id,
			String appName,
			String page,
			String actionName,
			String displayName,
			Action.ActionType actionType,
			PathPattern pathPattern,
			Set<String> allowedRoleNames
	) {
		this.id = id;
		this.appName = appName;
		this.page = page;
		this.actionName = actionName;
		this.displayName = displayName;
		this.actionType = actionType;
		this.pathPattern = pathPattern;
		this.allowedRoleNames =
				(allowedRoleNames != null)
						? Set.copyOf(allowedRoleNames)
						: Set.of();
	}

	public boolean matches(String path) {
		return pathPattern.matches(PathContainer.parsePath(path));
	}

	public boolean isAllowedForRoles(Set<String> userRoleNames) {
		return CollectionUtils.containsAny(allowedRoleNames, userRoleNames);
	}
}

