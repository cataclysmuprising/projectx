package com.tamantaw.projectx.persistence.criteria;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.tamantaw.projectx.persistence.criteria.base.AbstractCriteria;
import com.tamantaw.projectx.persistence.entity.AdministratorLoginHistory;
import com.tamantaw.projectx.persistence.entity.QAdministratorLoginHistory;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString(callSuper = true)
public class AdministratorLoginHistoryCriteria extends AbstractCriteria<QAdministratorLoginHistory> {

	private Long administratorId;
	private String ipAddress;
	private String os;
	private String clientAgent;
	private LocalDateTime loginDateFrom;
	private LocalDateTime loginDateTo;

	private AdministratorCriteria administrator;

	@Override
	public Predicate getFilter() {
		return getFilter(QAdministratorLoginHistory.administratorLoginHistory);
	}

	@Override
	public Predicate getFilter(QAdministratorLoginHistory h) {

		BooleanBuilder predicate = commonFilter(h._super);

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

		if (StringUtils.isNotBlank(keyword)) {
			predicate.and(
					h.ipAddress.containsIgnoreCase(keyword)
							.or(h.os.containsIgnoreCase(keyword))
							.or(h.clientAgent.containsIgnoreCase(keyword))
			);
		}

		if (administrator != null) {

			Predicate adminFilter =
					administrator.getFilter(h.administrator);

			if (adminFilter != null) {
				predicate.and(adminFilter);
			}
		}

		return predicate;
	}

	@Override
	public Class<?> getObjectClass() {
		return AdministratorLoginHistory.class;
	}
}
