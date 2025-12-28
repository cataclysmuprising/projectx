package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.AdministratorLoginHistoryCriteria;
import com.tamantaw.projectx.persistence.entity.AdministratorLoginHistory;
import com.tamantaw.projectx.persistence.entity.QAdministratorLoginHistory;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class AdministratorLoginHistoryRepository
		extends AbstractRepositoryImpl<
		Long,
		AdministratorLoginHistory,
		QAdministratorLoginHistory,
		AdministratorLoginHistoryCriteria
		> {

	@PersistenceContext(unitName = EM_FACTORY)
	private EntityManager entityManager;

	public AdministratorLoginHistoryRepository() {
		super(AdministratorLoginHistory.class, Long.class);
	}

	@PostConstruct
	public void init() {
		initialize(entityManager);
	}
}
