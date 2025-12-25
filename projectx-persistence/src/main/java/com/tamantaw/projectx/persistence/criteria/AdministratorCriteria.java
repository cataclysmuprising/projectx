package com.tamantaw.projectx.persistence.criteria;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import com.tamantaw.projectx.persistence.criteria.base.AbstractCriteria;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.QAdministrator;
import com.tamantaw.projectx.persistence.entity.QAdministratorRole;
import com.tamantaw.projectx.persistence.entity.QRole;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorCriteria extends AbstractCriteria<QAdministrator, Long> {

	private String name;
	private String loginId;
	private Administrator.Status status;

	private RoleCriteria role;

	// ----------------------------------------------------------------------
	// FILTER
	// ----------------------------------------------------------------------

	@Override
	public Predicate getFilter() {
		return getFilter(QAdministrator.administrator);
	}

	@Override
	public Predicate getFilter(QAdministrator a) {

		BooleanBuilder predicate = commonFilter(a._super);

		if (StringUtils.isNotBlank(name)) {
			predicate.and(a.name.eq(name));
		}

		if (StringUtils.isNotBlank(loginId)) {
			predicate.and(a.loginId.eq(loginId));
		}

		if (status != null) {
			predicate.and(a.status.eq(status));
		}

		// ---------------- KEYWORD SEARCH ----------------
		if (StringUtils.isNotBlank(keyword)) {
			predicate.and(
					a.name.containsIgnoreCase(keyword)
							.or(a.loginId.containsIgnoreCase(keyword))
			);
		}

		// ---------------- ROLE FILTER (EXISTS) ----------------
		if (role != null) {

			QAdministratorRole ar = QAdministratorRole.administratorRole;
			QRole r = QRole.role;

			BooleanBuilder roleFilter =
					(BooleanBuilder) role.getFilter(r);

			if (roleFilter.hasValue()) {
				predicate.and(
						JPAExpressions
								.selectOne()
								.from(ar)
								.join(ar.role, r)
								.where(ar.administrator.eq(a).and(roleFilter))
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
		return Administrator.class;
	}
}


