package com.tamantaw.projectx.persistence.criteria;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import com.tamantaw.projectx.persistence.criteria.base.AbstractCriteria;
import com.tamantaw.projectx.persistence.entity.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@ToString(callSuper = true)
public class RoleCriteria extends AbstractCriteria<QRole, Long> {

	private String appName;
	private String name;
	private Role.RoleType roleType;

	private AdministratorCriteria administrator;
	private ActionCriteria action;

	// ----------------------------------------------------------------------
	// FILTER
	// ----------------------------------------------------------------------

	@Override
	public Predicate getFilter() {
		return getFilter(QRole.role);
	}

	@Override
	public Predicate getFilter(QRole r) {

		BooleanBuilder predicate = commonFilter(r._super);

		if (StringUtils.isNotBlank(appName)) {
			predicate.and(r.appName.eq(appName));
		}

		if (StringUtils.isNotBlank(name)) {
			predicate.and(r.name.eq(name));
		}

		if (roleType != null) {
			predicate.and(r.roleType.eq(roleType));
		}

		// --------------------------------------------------------------
		// KEYWORD SEARCH
		// --------------------------------------------------------------
		if (StringUtils.isNotBlank(keyword)) {
			predicate.and(
					r.appName.containsIgnoreCase(keyword)
							.or(r.name.containsIgnoreCase(keyword))
							.or(r.description.containsIgnoreCase(keyword))
			);
		}

		// --------------------------------------------------------------
		// ACTION FILTER (EXISTS)
		// --------------------------------------------------------------
		if (action != null) {

			QRoleAction ra = QRoleAction.roleAction;
			QAction a = QAction.action;

			BooleanBuilder actionFilter =
					(BooleanBuilder) action.getFilter(a);

			if (actionFilter.hasValue()) {
				predicate.and(
						JPAExpressions
								.selectOne()
								.from(ra)
								.join(ra.action, a)
								.where(ra.role.eq(r).and(actionFilter))
								.exists()
				);
			}
		}

		// --------------------------------------------------------------
		// ADMINISTRATOR FILTER (EXISTS)
		// --------------------------------------------------------------
		if (administrator != null) {

			QAdministratorRole ar = QAdministratorRole.administratorRole;
			QAdministrator admin = QAdministrator.administrator;

			BooleanBuilder adminFilter =
					(BooleanBuilder) administrator.getFilter(admin);

			if (adminFilter.hasValue()) {
				predicate.and(
						JPAExpressions
								.selectOne()
								.from(ar)
								.join(ar.administrator, admin)
								.where(ar.role.eq(r).and(adminFilter))
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
		return Role.class;
	}
}
