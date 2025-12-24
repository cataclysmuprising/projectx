package com.tamantaw.projectx.persistence.criteria;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import com.tamantaw.projectx.persistence.criteria.base.AbstractCriteria;
import com.tamantaw.projectx.persistence.entity.QAction;
import com.tamantaw.projectx.persistence.entity.QRole;
import com.tamantaw.projectx.persistence.entity.QRoleAction;
import com.tamantaw.projectx.persistence.entity.RoleAction;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class RoleActionCriteria extends AbstractCriteria<QRoleAction> {

	private Long roleId;
	private Long actionId;

	private RoleCriteria role;
	private ActionCriteria action;

	// ----------------------------------------------------------------------
	// FILTER
	// ----------------------------------------------------------------------

	@Override
	public Predicate getFilter() {
		return getFilter(QRoleAction.roleAction);
	}

	@Override
	public Predicate getFilter(QRoleAction ra) {

		BooleanBuilder predicate = commonFilter(ra._super);

		if (roleId != null) {
			predicate.and(ra.roleId.eq(roleId));
		}

		if (actionId != null) {
			predicate.and(ra.actionId.eq(actionId));
		}

		// --------------------------------------------------------------
		// ROLE FILTER (EXISTS)
		// --------------------------------------------------------------
		if (role != null) {

			QRole r = QRole.role;

			BooleanBuilder roleFilter =
					(BooleanBuilder) role.getFilter(r);

			if (roleFilter.hasValue()) {
				predicate.and(
						JPAExpressions
								.selectOne()
								.from(r)
								.where(r.eq(ra.role).and(roleFilter))
								.exists()
				);
			}
		}

		// --------------------------------------------------------------
		// ACTION FILTER (EXISTS)
		// --------------------------------------------------------------
		if (action != null) {

			QAction a = QAction.action;

			BooleanBuilder actionFilter =
					(BooleanBuilder) action.getFilter(a);

			if (actionFilter.hasValue()) {
				predicate.and(
						JPAExpressions
								.selectOne()
								.from(a)
								.where(a.eq(ra.action).and(actionFilter))
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
		return RoleAction.class;
	}
}


