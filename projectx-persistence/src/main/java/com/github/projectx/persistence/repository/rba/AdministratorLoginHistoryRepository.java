package com.github.projectx.persistence.repository.rba;

import com.github.projectx.persistence.criteria.rba.AdministratorLoginHistoryCriteria;
import com.github.projectx.persistence.entity.rba.AdministratorLoginHistory;
import com.github.projectx.persistence.entity.rba.QAdministratorLoginHistory;
import com.github.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import static com.github.projectx.persistence.config.PersistenceContextNames.PERSISTENCE_UNIT;

@Repository
public class AdministratorLoginHistoryRepository
		extends AbstractRepositoryImpl<
		Long,
		AdministratorLoginHistory,
		QAdministratorLoginHistory,
		AdministratorLoginHistoryCriteria
		> {

	@PersistenceContext(unitName = PERSISTENCE_UNIT)
	private @Nullable EntityManager entityManager;

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 */
	public AdministratorLoginHistoryRepository() {
		super(AdministratorLoginHistory.class, Long.class);
	}

	/**
	 * Encapsulates deterministic data access for `init` so callers do not duplicate query logic across services.
	 */
	@PostConstruct
	public void init() {
		initialize(entityManager);
	}
}

