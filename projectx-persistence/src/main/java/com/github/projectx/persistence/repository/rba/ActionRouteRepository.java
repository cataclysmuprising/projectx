package com.github.projectx.persistence.repository.rba;

import com.github.projectx.persistence.criteria.rba.ActionRouteCriteria;
import com.github.projectx.persistence.entity.rba.ActionRoute;
import com.github.projectx.persistence.entity.rba.QAction;
import com.github.projectx.persistence.entity.rba.QActionRoute;
import com.github.projectx.persistence.entity.rba.QActionRouteTarget;
import com.github.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.List;

import static com.github.projectx.persistence.config.PersistenceContextNames.PERSISTENCE_UNIT;

@Repository
public class ActionRouteRepository
		extends AbstractRepositoryImpl<
		Long,
		ActionRoute,
		QActionRoute,
		ActionRouteCriteria
		> {

	private static final QActionRoute qActionRoute = QActionRoute.actionRoute;
	private static final QActionRouteTarget qActionRouteTarget = QActionRouteTarget.actionRouteTarget;
	private static final QAction qAction = QAction.action;

	@PersistenceContext(unitName = PERSISTENCE_UNIT)
	private @Nullable EntityManager entityManager;

	public ActionRouteRepository() {
		super(ActionRoute.class, Long.class);
	}

	@PostConstruct
	public void init() {
		initialize(entityManager);
	}

	public List<ActionRoute> findActiveRoutesByAppName(String appName) {
		assertInitialized();
		Assert.hasText(appName, "appName must not be blank");

		return queryFactory
				.selectDistinct(qActionRoute)
				.from(qActionRoute)
				.leftJoin(qActionRoute.routeTargets, qActionRouteTarget).fetchJoin()
				.leftJoin(qActionRouteTarget.action, qAction).fetchJoin()
				.where(
						qActionRoute.appName.eq(appName),
						qActionRoute.active.isTrue()
				)
				.orderBy(
						qActionRoute.priority.desc(),
						qActionRoute.id.asc()
				)
				.fetch();
	}

	public List<ActiveRouteBindingRow> findActiveRouteBindingRowsByAppName(String appName) {
		assertInitialized();
		Assert.hasText(appName, "appName must not be blank");

		return queryFactory
				.select(
						qActionRoute.id,
						qActionRoute.appName,
						qActionRoute.routePattern,
						qActionRoute.httpMethod,
						qActionRoute.routeKind,
						qActionRoute.priority,
						qActionRouteTarget.actionId,
						qAction.actionName
				)
				.from(qActionRoute)
				.join(qActionRoute.routeTargets, qActionRouteTarget)
				.join(qActionRouteTarget.action, qAction)
				.where(
						qActionRoute.appName.eq(appName),
						qActionRoute.active.isTrue()
				)
				.orderBy(
						qActionRoute.priority.desc(),
						qActionRoute.id.asc(),
						qActionRouteTarget.actionId.asc()
				)
				.fetch()
				.stream()
				.map(tuple -> new ActiveRouteBindingRow(
						tuple.get(qActionRoute.id),
						tuple.get(qActionRoute.appName),
						tuple.get(qActionRoute.routePattern),
						tuple.get(qActionRoute.httpMethod),
						tuple.get(qActionRoute.routeKind),
						tuple.get(qActionRoute.priority),
						tuple.get(qActionRouteTarget.actionId),
						tuple.get(qAction.actionName)
				))
				.toList();
	}

	public List<RouteBindingRow> findRouteBindingsByActionIds(Collection<Long> actionIds) {
		assertInitialized();
		Assert.notNull(actionIds, "actionIds must not be null");
		if (actionIds.isEmpty()) {
			return List.of();
		}

		return queryFactory
				.select(
						qActionRouteTarget.actionId,
						qActionRoute.routePattern,
						qActionRoute.routeKind
				)
				.from(qActionRouteTarget)
				.join(qActionRouteTarget.route, qActionRoute)
				.where(
						qActionRouteTarget.actionId.in(actionIds),
						qActionRoute.active.isTrue()
				)
				.fetch()
				.stream()
				.map(tuple -> new RouteBindingRow(
						tuple.get(qActionRouteTarget.actionId),
						tuple.get(qActionRoute.routePattern),
						tuple.get(qActionRoute.routeKind)
				))
				.toList();
	}

	public record RouteBindingRow(
			Long actionId,
			String routePattern,
			ActionRoute.RouteKind routeKind
	) {
	}

	public record ActiveRouteBindingRow(
			Long routeId,
			String appName,
			String routePattern,
			String httpMethod,
			ActionRoute.RouteKind routeKind,
			Integer priority,
			Long actionId,
			String actionName
	) {
	}
}
