package com.tamantaw.projectx.persistence.criteria;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import com.tamantaw.projectx.persistence.criteria.base.AbstractCriteria;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.QAction;
import com.tamantaw.projectx.persistence.entity.QRole;
import com.tamantaw.projectx.persistence.entity.QRoleAction;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@ToString(callSuper = true)
public class ActionCriteria extends AbstractCriteria<QAction, Long> {

	private String appName;
	private String page;
	private String actionName;
	private String displayName;
	private Action.ActionType actionType;
	private String url;

	private RoleCriteria role;

	// ----------------------------------------------------------------------
	// FILTER
	// ----------------------------------------------------------------------

	@Override
	public Predicate getFilter() {
		return getFilter(QAction.action);
	}

	@Override
	public Predicate getFilter(QAction a) {

		BooleanBuilder predicate = commonFilter(a._super);

		if (StringUtils.isNotBlank(appName)) {
			predicate.and(a.appName.eq(appName));
		}

		if (StringUtils.isNotBlank(page)) {
			predicate.and(a.page.eq(page));
		}

		if (StringUtils.isNotBlank(actionName)) {
			predicate.and(a.actionName.eq(actionName));
		}

		if (StringUtils.isNotBlank(displayName)) {
			predicate.and(a.displayName.eq(displayName));
		}

		if (actionType != null) {
			predicate.and(a.actionType.eq(actionType));
		}

		if (StringUtils.isNotBlank(url)) {
			predicate.and(a.url.eq(url));
		}

		// Keyword search (explicit, local responsibility)
		if (StringUtils.isNotBlank(keyword)) {
			predicate.and(
					a.appName.containsIgnoreCase(keyword)
							.or(a.page.containsIgnoreCase(keyword))
							.or(a.actionName.containsIgnoreCase(keyword))
							.or(a.displayName.containsIgnoreCase(keyword))
							.or(a.url.containsIgnoreCase(keyword))
							.or(a.description.containsIgnoreCase(keyword))
			);
		}

		// ---------------- ROLE FILTER (EXISTS) ----------------
		if (role != null) {

			QRoleAction ra = QRoleAction.roleAction;
			QRole r = QRole.role;

			BooleanBuilder roleFilter =
					(BooleanBuilder) role.getFilter(r);

			if (roleFilter.hasValue()) {
				predicate.and(
						JPAExpressions
								.selectOne()
								.from(ra)
								.join(ra.role, r)
								.where(ra.action.eq(a).and(roleFilter))
								.exists()
				);
			}
		}

		return predicate;
	}

	// ----------------------------------------------------------------------
	// META
	// ----------------------------------------------------------------------

	@Override
	public Class<?> getObjectClass() {
		return Action.class;
	}
}
