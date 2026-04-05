package com.github.projectx.persistence.criteria.rba;

import com.github.projectx.persistence.criteria.base.AbstractCriteria;
import com.github.projectx.persistence.entity.rba.ActionRoute;
import com.github.projectx.persistence.entity.rba.QAction;
import com.github.projectx.persistence.entity.rba.QActionRoute;
import com.github.projectx.persistence.entity.rba.QActionRouteTarget;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@ToString(callSuper = true)
public class ActionRouteCriteria extends AbstractCriteria<QActionRoute> {

	private String appName;
	private String routePattern;
	private String httpMethod;
	private ActionRoute.RouteKind routeKind;
	private Boolean active;
	private ActionCriteria action;

	@Override
	public Predicate getFilter() {
		return getFilter(QActionRoute.actionRoute);
	}

	@Override
	public Predicate getFilter(QActionRoute route) {
		BooleanBuilder predicate = commonFilter(route._super);

		if (StringUtils.isNotBlank(appName)) {
			predicate.and(route.appName.eq(appName));
		}

		if (StringUtils.isNotBlank(routePattern)) {
			predicate.and(route.routePattern.eq(routePattern));
		}

		if (StringUtils.isNotBlank(httpMethod)) {
			predicate.and(route.httpMethod.eq(httpMethod));
		}

		if (routeKind != null) {
			predicate.and(route.routeKind.eq(routeKind));
		}

		if (active != null) {
			predicate.and(route.active.eq(active));
		}

		if (StringUtils.isNotBlank(keyword)) {
			predicate.and(
					route.routePattern.containsIgnoreCase(keyword)
							.or(route.httpMethod.containsIgnoreCase(keyword))
							.or(route.description.containsIgnoreCase(keyword))
			);
		}

		if (action != null) {
			QActionRouteTarget routeTarget = QActionRouteTarget.actionRouteTarget;
			QAction actionRoot = QAction.action;

			BooleanBuilder actionFilter = (BooleanBuilder) action.getFilter(actionRoot);
			if (actionFilter.hasValue()) {
				predicate.and(
						JPAExpressions
								.selectOne()
								.from(routeTarget)
								.join(routeTarget.action, actionRoot)
								.where(routeTarget.route.eq(route).and(actionFilter))
								.exists()
				);
			}
		}

		return predicate;
	}

	@Override
	public Class<?> getObjectClass() {
		return ActionRoute.class;
	}
}
