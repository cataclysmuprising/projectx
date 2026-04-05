package com.github.projectx.persistence.criteria.rba;

import com.github.projectx.persistence.criteria.base.AbstractCriteria;
import com.github.projectx.persistence.entity.rba.AdministratorRole;
import com.github.projectx.persistence.entity.rba.QAdministrator;
import com.github.projectx.persistence.entity.rba.QAdministratorRole;
import com.github.projectx.persistence.entity.rba.QRole;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
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

	/**
	 * Defines the criteria predicate contract so repositories apply the same business filters deterministically.
	 */
	@Override
	public Predicate getFilter() {
		return getFilter(QAdministratorRole.administratorRole);
	}

	/**
	 * Defines the criteria predicate contract so repositories apply the same business filters deterministically.
	 */
	@Override
	public Predicate getFilter(QAdministratorRole ar) {

		BooleanBuilder predicate = commonFilter(ar._super);

		if (administratorId != null) {
			predicate.and(ar.administratorId.eq(administratorId));
		}

		if (roleId != null) {
			predicate.and(ar.roleId.eq(roleId));
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


