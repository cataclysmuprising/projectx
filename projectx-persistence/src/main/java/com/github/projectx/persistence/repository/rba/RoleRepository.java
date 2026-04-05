package com.github.projectx.persistence.repository.rba;

import com.github.projectx.persistence.criteria.rba.RoleCriteria;
import com.github.projectx.persistence.entity.rba.QRole;
import com.github.projectx.persistence.entity.rba.Role;
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
public class RoleRepository
		extends AbstractRepositoryImpl<
		Long,
		Role,
		QRole,
		RoleCriteria
		> {

	private final QRole qEntity = QRole.role;

	@PersistenceContext(unitName = PERSISTENCE_UNIT)
	private @Nullable EntityManager entityManager;

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 */
	public RoleRepository() {
		super(Role.class, Long.class);
	}

	/**
	 * Encapsulates deterministic data access for `init` so callers do not duplicate query logic across services.
	 */
	@PostConstruct
	public void init() {
		initialize(entityManager);
	}

	/**
	 * Resolves which role IDs currently exist in storage so callers can validate relation inputs
	 * with a single query and avoid N+1 existence checks.
	 */
	public Set<Long> findExistingIds(Collection<Long> roleIds) {
		assertInitialized();
		Assert.notNull(roleIds, "roleIds must not be null");
		if (roleIds.isEmpty()) {
			return Set.of();
		}

		return new HashSet<>(queryFactory
				.select(qEntity.id)
				.from(qEntity)
				.where(qEntity.id.in(roleIds))
				.fetch());
	}
}

