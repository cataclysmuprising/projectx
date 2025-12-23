package com.tamantaw.projectx.backend.utils;

import com.tamantaw.projectx.backend.BackendApplication;
import com.tamantaw.projectx.backend.dto.ActionDefinition;
import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.ActionService;
import com.tamantaw.projectx.persistence.service.RoleService;
import jakarta.annotation.PostConstruct;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ActionRegistry {

	private final ActionService actionService;
	private final RoleService roleService;
	private final PathPatternParser patternParser = new PathPatternParser();

	private volatile List<ActionDefinition> actions = List.of();

	public ActionRegistry(ActionService actionService, RoleService roleService) {
		this.actionService = actionService;
		this.roleService = roleService;
	}

	@PostConstruct
	public void load() {
		reload();
	}

	public void reload() {

		ActionCriteria criteria = new ActionCriteria();
		criteria.setAppName(BackendApplication.APP_NAME);

		List<ActionDTO> dbActions;
		try {
			dbActions = actionService.findAll(criteria);
		}
		catch (PersistenceException e) {
			throw new RuntimeException(e);
		}

		Set<Long> actionIds = dbActions.stream()
				.map(ActionDTO::getId)
				.collect(Collectors.toSet());

		Map<Long, Set<String>> roleNamesByActionId;
		try {
			roleNamesByActionId =
					roleService.selectRoleNamesByActionIds(
							actionIds,
							BackendApplication.APP_NAME
					);
		}
		catch (PersistenceException e) {
			throw new RuntimeException(e);
		}

		actions = dbActions.stream()
				.map(a -> new ActionDefinition(
						a.getId(),
						a.getAppName(),
						a.getPage(),
						a.getActionName(),
						a.getDisplayName(),
						a.getActionType(),
						patternParser.parse(a.getUrl()),
						roleNamesByActionId.getOrDefault(
								a.getId(),
								Collections.emptySet()
						)
				))
				.toList();
	}

	public ActionDefinition resolve(String requestPath) {
		return actions.stream()
				.filter(action -> action.matches(requestPath))
				.findFirst()
				.orElse(null);
	}

	public Set<String> resolveAvailableActionsForUser(
			String appName,
			@Nullable String page,
			Set<String> userRoleNames
	) {
		return actions.stream()
				.filter(a -> appName.equals(a.appName()))
				.filter(a ->
						a.actionType() == Action.ActionType.MAIN
								|| (page != null
								&& a.actionType() == Action.ActionType.SUB
								&& page.equalsIgnoreCase(a.page()))
				)
				.filter(a -> a.isAllowedForRoles(userRoleNames))
				.map(ActionDefinition::actionName)
				.collect(Collectors.toSet());
	}
}
