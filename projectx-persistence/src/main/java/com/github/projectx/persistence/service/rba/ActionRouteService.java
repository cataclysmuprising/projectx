package com.github.projectx.persistence.service.rba;

import com.github.projectx.persistence.criteria.rba.ActionRouteCriteria;
import com.github.projectx.persistence.dto.rba.ActionDTO;
import com.github.projectx.persistence.dto.rba.ActionRouteDTO;
import com.github.projectx.persistence.entity.rba.ActionRoute;
import com.github.projectx.persistence.entity.rba.QActionRoute;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.mapper.rba.ActionRouteMapper;
import com.github.projectx.persistence.repository.rba.ActionRouteRepository;
import com.github.projectx.persistence.service.base.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActionRouteService
		extends BaseService<
		Long,
		ActionRoute,
		QActionRoute,
		ActionRouteCriteria,
		ActionRouteDTO,
		ActionRouteMapper> {

	private final ActionRouteRepository actionRouteRepository;

	public ActionRouteService(
			ActionRouteRepository actionRouteRepository,
			ActionRouteMapper mapper
	) {
		super(actionRouteRepository, mapper);
		this.actionRouteRepository = actionRouteRepository;
	}

	@Transactional(readOnly = true)
	public List<ActionRouteDTO> findActiveRoutesByAppName(String appName) throws PersistenceException {
		Assert.hasText(appName, "App Name shouldn't be blank.");
		try {
			return mapper.mapToDtoList(actionRouteRepository.findActiveRoutesByAppName(appName), mappingContext);
		}
		catch (Exception e) {
			throw new PersistenceException("Failed to fetch active action routes by appName=" + appName, e);
		}
	}

	@Transactional(readOnly = true)
	public List<ActiveRouteDefinitionView> findActiveRouteDefinitionsByAppName(String appName) throws PersistenceException {
		Assert.hasText(appName, "App Name shouldn't be blank.");
		try {
			Map<RouteDefinitionKey, RouteDefinitionAccumulator> groupedRoutes = new LinkedHashMap<>();

			for (ActionRouteRepository.ActiveRouteBindingRow row : actionRouteRepository.findActiveRouteBindingRowsByAppName(appName)) {
				RouteDefinitionKey key = new RouteDefinitionKey(
						row.routeId(),
						row.appName(),
						row.routePattern(),
						row.httpMethod(),
						row.routeKind(),
						row.priority()
				);
				RouteDefinitionAccumulator accumulator = groupedRoutes.computeIfAbsent(key, ignored -> new RouteDefinitionAccumulator());

				if (row.actionId() != null) {
					accumulator.actionIds().add(row.actionId());
				}
				if (row.actionName() != null && !row.actionName().isBlank()) {
					accumulator.actionNames().add(row.actionName().trim());
				}
			}

			return groupedRoutes.entrySet().stream()
					.map(entry -> new ActiveRouteDefinitionView(
							entry.getKey().routeId(),
							entry.getKey().appName(),
							entry.getKey().routePattern(),
							entry.getKey().httpMethod(),
							entry.getKey().routeKind(),
							entry.getKey().priority(),
							Set.copyOf(entry.getValue().actionIds()),
							Set.copyOf(entry.getValue().actionNames())
					))
					.toList();
		}
		catch (Exception e) {
			throw new PersistenceException("Failed to fetch active action route definitions by appName=" + appName, e);
		}
	}

	@Transactional(readOnly = true)
	public void applyCoverage(List<ActionDTO> actions) throws PersistenceException {
		if (actions == null || actions.isEmpty()) {
			return;
		}

		Set<Long> actionIds = actions.stream()
				.map(ActionDTO::getId)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());
		if (actionIds.isEmpty()) {
			return;
		}

		try {
			Map<Long, ActionRouteCoverage> coverageByActionId = actionRouteRepository.findRouteBindingsByActionIds(actionIds)
					.stream()
					.collect(Collectors.groupingBy(
							ActionRouteRepository.RouteBindingRow::actionId,
							Collectors.collectingAndThen(Collectors.toList(), this::toCoverage)
					));

			for (ActionDTO action : actions) {
				if (action == null || action.getId() == null) {
					continue;
				}

				ActionRouteCoverage coverage = coverageByActionId.get(action.getId());
				if (coverage == null) {
					action.setPrimaryRoute(action.getUrl());
					action.setTotalRouteCount(action.getUrl() == null || action.getUrl().isBlank() ? 0L : 1L);
					action.setPageSupportRouteCount(0L);
					action.setSharedLookupRouteCount(0L);
					continue;
				}

				action.setPrimaryRoute(coverage.primaryRoute());
				action.setTotalRouteCount(coverage.totalRouteCount());
				action.setPageSupportRouteCount(coverage.pageSupportRouteCount());
				action.setSharedLookupRouteCount(coverage.sharedLookupRouteCount());
			}
		}
		catch (Exception e) {
			throw new PersistenceException("Failed to apply action route coverage", e);
		}
	}

	private ActionRouteCoverage toCoverage(List<ActionRouteRepository.RouteBindingRow> rows) {
		String primaryRoute = rows.stream()
				.filter(row -> row.routeKind() == ActionRoute.RouteKind.PRIMARY)
				.map(ActionRouteRepository.RouteBindingRow::routePattern)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);

		long pageSupportCount = rows.stream()
				.filter(row -> row.routeKind() == ActionRoute.RouteKind.PAGE_SUPPORT)
				.count();

		long sharedLookupCount = rows.stream()
				.filter(row -> row.routeKind() == ActionRoute.RouteKind.SHARED_LOOKUP)
				.count();

		return new ActionRouteCoverage(
				primaryRoute,
				(long) rows.size(),
				pageSupportCount,
				sharedLookupCount
		);
	}

	private record ActionRouteCoverage(
			String primaryRoute,
			long totalRouteCount,
			long pageSupportRouteCount,
			long sharedLookupRouteCount
	) {
	}

	public record ActiveRouteDefinitionView(
			Long routeId,
			String appName,
			String routePattern,
			String httpMethod,
			ActionRoute.RouteKind routeKind,
			Integer priority,
			Set<Long> actionIds,
			Set<String> actionNames
	) {
	}

	private record RouteDefinitionKey(
			Long routeId,
			String appName,
			String routePattern,
			String httpMethod,
			ActionRoute.RouteKind routeKind,
			Integer priority
	) {
	}

	private record RouteDefinitionAccumulator(
			Set<Long> actionIds,
			Set<String> actionNames
	) {
		private RouteDefinitionAccumulator() {
			this(new TreeSet<>(), new TreeSet<>());
		}
	}
}
