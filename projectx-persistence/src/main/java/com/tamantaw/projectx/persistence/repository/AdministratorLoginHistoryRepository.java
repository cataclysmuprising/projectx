package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.AdministratorLoginHistoryCriteria;
import com.tamantaw.projectx.persistence.entity.AdministratorLoginHistory;
import com.tamantaw.projectx.persistence.entity.QAdministratorLoginHistory;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class AdministratorLoginHistoryRepository
		extends AbstractRepositoryImpl<
		AdministratorLoginHistory,
		QAdministratorLoginHistory,
		AdministratorLoginHistoryCriteria,
		Long> {

	public AdministratorLoginHistoryRepository(@Qualifier(EM_FACTORY) EntityManager entityManager) {
		super(AdministratorLoginHistory.class, entityManager);
	}
}
