package com.github.projectx.backend.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.projectx.backend.BackendApplication;
import com.github.projectx.backend.dto.ActionDefinition;
import com.github.projectx.backend.dto.RouteAccessDefinition;
import com.github.projectx.persistence.criteria.rba.ActionCriteria;
import com.github.projectx.persistence.dto.rba.ActionDTO;
import com.github.projectx.persistence.entity.rba.Action;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.service.rba.ActionRouteService;
import com.github.projectx.persistence.service.rba.ActionService;
import com.github.projectx.persistence.service.rba.RoleService;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPatternParser;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ActionRegistry {

	private static final Logger logger = LogManager.getLogger(ActionRegistry.class);
	private static final PathPatternParser PATTERN_PARSER = new PathPatternParser();
	private static final long STALE_REFRESH_INTERVAL_MILLIS = Duration.ofSeconds(5).toMillis();

	private final ActionService actionService;
	private final ActionRouteService actionRouteService;
	private final RoleService roleService;
	private final Object reloadMonitor = new Object();

	private final Cache<RouteResolveCacheKey, Optional<RouteAccessDefinition>> resolveCache = Caffeine.newBuilder()
			.maximumSize(4_096)
			.expireAfterAccess(Duration.ofMinutes(15))
			.build();

	private final Cache<AvailableActionsCacheKey, Set<String>> availableActionsCache = Caffeine.newBuilder()
			.maximumSize(2_048)
			.expireAfterAccess(Duration.ofMinutes(10))
			.build();

	private volatile List<ActionDefinition> actions = List.of();
	private volatile List<RouteAccessDefinition> routes = List.of();
	private volatile long lastReloadEpochMillis;

	public ActionRegistry(
			ActionService actionService,
			ActionRouteService actionRouteService,
			RoleService roleService
	) {
		this.actionService = actionService;
		this.actionRouteService = actionRouteService;
		this.roleService = roleService;
	}

	@PostConstruct
	public void load() {
		reload();
	}

	public void reload() {
		synchronized (reloadMonitor) {
			reloadInternal();
		}
	}

	public void refreshIfStale() {
		long now = System.currentTimeMillis();
		if (!isRefreshDue(now)) {
			return;
		}

		synchronized (reloadMonitor) {
			now = System.currentTimeMillis();
			if (!isRefreshDue(now)) {
				return;
			}

			long previousReloadEpochMillis = lastReloadEpochMillis;
			try {
				reloadInternal();
				long staleAgeMillis = previousReloadEpochMillis <= 0L
						? -1L
						: Math.max(0L, now - previousReloadEpochMillis);
				logger.debug("[action-registry] stale refresh applied [previousAgeMs={}]", staleAgeMillis);
			}
			catch (RuntimeException e) {
				logger.warn("[action-registry] stale refresh failed; keeping last known action registry snapshot", e);
			}
		}
	}

	private void reloadInternal() {
		ActionCriteria criteria = new ActionCriteria();
		criteria.setAppName(BackendApplication.APP_NAME);

		List<ActionDTO> dbActions;
		try {
			dbActions = actionService.findAll(criteria);
		}
		catch (PersistenceException e) {
			throw new IllegalStateException("Failed to load action definitions", e);
		}

		if (dbActions.isEmpty()) {
			actions = List.of();
			routes = List.of();
			invalidateCaches();
			lastReloadEpochMillis = System.currentTimeMillis();
			logger.info("[action-registry] loaded 0 action definitions and 0 route definitions");
			return;
		}

		Map<Long, Set<String>> roleNamesByActionId;
		try {
			roleNamesByActionId = roleService.selectRoleNamesByAppName(BackendApplication.APP_NAME);
		}
		catch (PersistenceException e) {
			throw new IllegalStateException("Failed to load role names by app name", e);
		}

		List<ActionDefinition> loadedActions = dbActions.stream()
				.map(action -> toActionDefinition(action, roleNamesByActionId))
				.flatMap(Optional::stream)
				.toList();

		List<RouteAccessDefinition> loadedRoutes;
		try {
			loadedRoutes = actionRouteService.findActiveRouteDefinitionsByAppName(BackendApplication.APP_NAME).stream()
					.map(route -> toRouteAccessDefinition(route, roleNamesByActionId))
					.flatMap(Optional::stream)
					.toList();
		}
		catch (PersistenceException e) {
			throw new IllegalStateException("Failed to load action route definitions", e);
		}

		actions = loadedActions;
		routes = loadedRoutes;
		invalidateCaches();
		lastReloadEpochMillis = System.currentTimeMillis();
		logger.info(
				"[action-registry] loaded {} action definitions and {} route definitions",
				actions.size(),
				routes.size()
		);
	}

	@Nullable
	public RouteAccessDefinition resolve(String requestPath) {
		return resolve(requestPath, "ANY");
	}

	@Nullable
	public RouteAccessDefinition resolve(String requestPath, @Nullable String requestMethod) {
		refreshIfStale();

		if (requestPath == null || requestPath.isBlank()) {
			return null;
		}

		RouteResolveCacheKey cacheKey = new RouteResolveCacheKey(
				normalizeMethod(requestMethod),
				requestPath.trim()
		);

		return resolveCache.get(cacheKey, key -> resolveRoute(key.requestMethod(), key.requestPath()))
				.orElse(null);
	}

	public Set<String> resolveAvailableActionsForUser(
			String appName,
			@Nullable String page,
			Set<String> userRoleNames
	) {
		refreshIfStale();

		String normalizedAppName = appName == null ? "" : appName.trim();
		if (normalizedAppName.isBlank()) {
			return Set.of();
		}

		String normalizedPage = page == null ? "" : page.trim().toLowerCase(Locale.ROOT);
		Set<String> normalizedRoles = userRoleNames == null
				? Set.of()
				: userRoleNames.stream()
				.filter(Objects::nonNull)
				.map(String::trim)
				.filter(role -> !role.isBlank())
				.collect(Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new), Set::copyOf));

		AvailableActionsCacheKey cacheKey = new AvailableActionsCacheKey(normalizedAppName, normalizedPage, normalizedRoles);
		return availableActionsCache.get(cacheKey, key -> actions.stream()
				.filter(a -> normalizedAppName.equals(a.appName()))
				.filter(a ->
						a.actionType() == Action.ActionType.MAIN
								|| (!normalizedPage.isBlank()
								&& a.actionType() == Action.ActionType.SUB
								&& normalizedPage.equalsIgnoreCase(a.page()))
				)
				.filter(a -> a.isAllowedForRoles(normalizedRoles))
				.map(ActionDefinition::actionName)
				.collect(Collectors.toUnmodifiableSet()));
	}

	private Optional<ActionDefinition> toActionDefinition(
			ActionDTO dto,
			Map<Long, Set<String>> roleNamesByActionId
	) {
		if (dto.getUrl() == null || dto.getUrl().isBlank()) {
			logger.warn("[action-registry] skipped action id={} due to empty URL", dto.getId());
			return Optional.empty();
		}

		try {
			return Optional.of(new ActionDefinition(
					dto.getId(),
					dto.getAppName(),
					dto.getPage(),
					dto.getActionName(),
					dto.getDisplayName(),
					dto.getActionType(),
					PATTERN_PARSER.parse(dto.getUrl()),
					roleNamesByActionId.getOrDefault(dto.getId(), Collections.emptySet())
			));
		}
		catch (RuntimeException e) {
			logger.warn(
					"[action-registry] skipped invalid action pattern id={} url={}",
					dto.getId(),
					dto.getUrl(),
					e
			);
			return Optional.empty();
		}
	}

	private Optional<RouteAccessDefinition> toRouteAccessDefinition(
			ActionRouteService.ActiveRouteDefinitionView dto,
			Map<Long, Set<String>> roleNamesByActionId
	) {
		if (dto.routePattern() == null || dto.routePattern().isBlank()) {
			logger.warn("[action-registry] skipped route id={} due to empty routePattern", dto.routeId());
			return Optional.empty();
		}

		if (dto.actionIds() == null || dto.actionIds().isEmpty()) {
			logger.warn(
					"[action-registry] skipped route id={} pattern={} due to empty target actions",
					dto.routeId(),
					dto.routePattern()
			);
			return Optional.empty();
		}

		try {
			Set<Long> targetActionIds = dto.actionIds().stream()
					.filter(Objects::nonNull)
					.collect(Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new), Set::copyOf));

			Set<String> targetActionNames = dto.actionNames().stream()
					.filter(Objects::nonNull)
					.map(String::trim)
					.filter(name -> !name.isBlank())
					.collect(Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new), Set::copyOf));

			Set<String> allowedRoleNames = targetActionIds.stream()
					.flatMap(actionId -> roleNamesByActionId.getOrDefault(actionId, Set.of()).stream())
					.collect(Collectors.collectingAndThen(Collectors.toCollection(TreeSet::new), Set::copyOf));

			return Optional.of(new RouteAccessDefinition(
					dto.routeId(),
					dto.appName(),
					dto.httpMethod(),
					dto.routePattern(),
					dto.routeKind(),
					dto.priority(),
					PATTERN_PARSER.parse(dto.routePattern()),
					targetActionIds,
					targetActionNames,
					allowedRoleNames
			));
		}
		catch (RuntimeException e) {
			logger.warn(
					"[action-registry] skipped invalid route pattern id={} routePattern={}",
					dto.routeId(),
					dto.routePattern(),
					e
			);
			return Optional.empty();
		}
	}

	private Optional<RouteAccessDefinition> resolveRoute(String requestMethod, String requestPath) {
		PathContainer parsedPath = PathContainer.parsePath(requestPath);

		Optional<RouteAccessDefinition> exactMethodMatch = routes.stream()
				.filter(route -> route.matchesExactMethod(requestMethod, parsedPath))
				.findFirst();
		if (exactMethodMatch.isPresent()) {
			return exactMethodMatch;
		}

		return routes.stream()
				.filter(route -> route.matchesAnyMethod(parsedPath))
				.findFirst();
	}

	private void invalidateCaches() {
		resolveCache.invalidateAll();
		availableActionsCache.invalidateAll();
	}

	private boolean isRefreshDue(long now) {
		long lastReload = lastReloadEpochMillis;
		return lastReload <= 0L || now - lastReload >= STALE_REFRESH_INTERVAL_MILLIS;
	}

	private String normalizeMethod(@Nullable String requestMethod) {
		if (requestMethod == null || requestMethod.isBlank()) {
			return "ANY";
		}
		return requestMethod.trim().toUpperCase(Locale.ROOT);
	}

	private record RouteResolveCacheKey(String requestMethod, String requestPath) {
	}

	private record AvailableActionsCacheKey(String appName, String page, Set<String> normalizedRoles) {
	}
}
