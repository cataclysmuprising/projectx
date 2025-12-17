package com.tamantaw.projectx.persistence.criteria;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import com.tamantaw.projectx.persistence.criteria.base.AbstractCriteria;
import com.tamantaw.projectx.persistence.entity.AdministratorRole;
import com.tamantaw.projectx.persistence.entity.QAdministrator;
import com.tamantaw.projectx.persistence.entity.QAdministratorRole;
import com.tamantaw.projectx.persistence.entity.QRole;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorRoleCriteria extends AbstractCriteria<QAdministratorRole> {

	private Long administratorId;
	private Long roleId;

	private AdministratorCriteria administrator;
	private RoleCriteria role;

	// ----------------------------------------------------------------------
	// FILTER
	// ----------------------------------------------------------------------

	@Override
	public Predicate getFilter() {
		return getFilter(QAdministratorRole.administratorRole);
	}

	@Override
	public Predicate getFilter(QAdministratorRole ar) {

		BooleanBuilder predicate = commonFilter(ar._super);

		if (administratorId != null) {
			predicate.and(ar.administrator.id.eq(administratorId));
		}

		if (roleId != null) {
			predicate.and(ar.role.id.eq(roleId));
		}

		// --------------------------------------------------------------
		// ADMINISTRATOR FILTER (EXISTS)
		// --------------------------------------------------------------
		if (administrator != null) {

			QAdministrator a = QAdministrator.administrator;

			BooleanBuilder adminFilter =
					(BooleanBuilder) administrator.getFilter(a);

			if (adminFilter.hasValue()) {
				predicate.and(
						JPAExpressions
								.selectOne()
								.from(a)
								.where(a.eq(ar.administrator).and(adminFilter))
								.exists()
				);
			}
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
								.where(r.eq(ar.role).and(roleFilter))
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
		return AdministratorRole.class;
	}
}

