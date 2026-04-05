package com.github.projectx.persistence.repository.rba;

import com.github.projectx.persistence.criteria.rba.AdministratorCriteria;
import com.github.projectx.persistence.entity.rba.Administrator;
import com.github.projectx.persistence.entity.rba.QAdministrator;
import com.github.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.github.projectx.persistence.config.PersistenceContextNames.PERSISTENCE_UNIT;

@Repository
public class AdministratorRepository
		extends AbstractRepositoryImpl<
		Long,
		Administrator,
		QAdministrator,
		AdministratorCriteria
		> {

	private static final QAdministrator qAdministrator = QAdministrator.administrator;

	@PersistenceContext(unitName = PERSISTENCE_UNIT)
	private @Nullable EntityManager entityManager;

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 */
	public AdministratorRepository() {
		super(Administrator.class, Long.class);
	}

	/**
	 * Encapsulates deterministic data access for `init` so callers do not duplicate query logic across services.
	 */
	@PostConstruct
	public void init() {
		initialize(entityManager);
	}

	/**
	 * Resolves which administrator IDs currently exist in storage so callers can validate
	 * relation inputs with one deterministic query.
	 */
	public Set<Long> findExistingIds(Collection<Long> administratorIds) {
		assertInitialized();
		Assert.notNull(administratorIds, "administratorIds must not be null");
		if (administratorIds.isEmpty()) {
			return Set.of();
		}

		return new HashSet<>(queryFactory
				.select(qAdministrator.id)
				.from(qAdministrator)
				.where(qAdministrator.id.in(administratorIds))
				.fetch());
	}
}


