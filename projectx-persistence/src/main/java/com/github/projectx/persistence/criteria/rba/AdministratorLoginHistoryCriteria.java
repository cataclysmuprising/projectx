package com.github.projectx.persistence.criteria.rba;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.projectx.persistence.criteria.base.AbstractCriteria;
import com.github.projectx.persistence.entity.rba.AdministratorLoginHistory;
import com.github.projectx.persistence.entity.rba.QAdministrator;
import com.github.projectx.persistence.entity.rba.QAdministratorLoginHistory;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.JPAExpressions;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorLoginHistoryCriteria extends AbstractCriteria<QAdministratorLoginHistory> {

	private Long administratorId;
	private String ipAddress;
	private String os;
	private String clientAgent;
	@JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	@DateTimeFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	private LocalDateTime loginDateFrom;
	@JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	@DateTimeFormat(pattern = "dd-MM-yyyy HH:mm:ss")
	private LocalDateTime loginDateTo;

	// Delegated criteria (cross-aggregate)
	private AdministratorCriteria administrator;

	/**
	 * Defines the criteria predicate contract so repositories apply the same business filters deterministically.
	 */
	@Override
	public Predicate getFilter() {
		return getFilter(QAdministratorLoginHistory.administratorLoginHistory);
	}

	/**
	 * Defines the criteria predicate contract so repositories apply the same business filters deterministically.
	 */
	@Override
	public Predicate getFilter(QAdministratorLoginHistory h) {

		BooleanBuilder predicate = commonFilter(h._super);

		// ------------------------------------------------------------
		// Scalar & FK filters (NO entity navigation)
		// ------------------------------------------------------------
		if (administratorId != null) {
			predicate.and(h.administratorId.eq(administratorId));
		}

		if (StringUtils.isNotBlank(ipAddress)) {
			predicate.and(h.ipAddress.eq(ipAddress));
		}

		if (StringUtils.isNotBlank(os)) {
			predicate.and(h.os.eq(os));
		}

		if (StringUtils.isNotBlank(clientAgent)) {
			predicate.and(h.clientAgent.eq(clientAgent));
		}

		if (loginDateFrom != null) {
			predicate.and(h.loginDate.goe(loginDateFrom));
		}

		if (loginDateTo != null) {
			predicate.and(h.loginDate.loe(loginDateTo));
		}

		// ------------------------------------------------------------
		// Keyword (local fields only)
		// ------------------------------------------------------------
		if (StringUtils.isNotBlank(keyword)) {
			predicate.and(
					h.ipAddress.containsIgnoreCase(keyword)
							.or(h.os.containsIgnoreCase(keyword))
							.or(h.clientAgent.containsIgnoreCase(keyword))
			);
		}

		// ------------------------------------------------------------
		// Administrator delegation (EXISTS, FK-based)
		// ------------------------------------------------------------
		if (administrator != null) {
			QAdministrator a = QAdministrator.administrator;
			Predicate adminFilter = administrator.getFilter(a);

			if (adminFilter != null) {
				predicate.and(
						JPAExpressions.selectOne()
								.from(a)
								.where(
										a.id.eq(h.administratorId),
										adminFilter
								)
								.exists()
				);
			}
		}

		return predicate;
	}

	@Override
	public Class<?> getObjectClass() {
		return AdministratorLoginHistory.class;
	}
}

