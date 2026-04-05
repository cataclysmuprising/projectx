package com.github.projectx.persistence.repository.rba;

import com.github.projectx.persistence.criteria.rba.ActionCriteria;
import com.github.projectx.persistence.entity.rba.Action;
import com.github.projectx.persistence.entity.rba.QAction;
import com.github.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.github.projectx.persistence.config.PersistenceContextNames.PERSISTENCE_UNIT;

@Repository
public class ActionRepository
		extends AbstractRepositoryImpl<
		Long,
		Action,
		QAction,
		ActionCriteria
		> {

	private static final QAction qAction = QAction.action;

	@PersistenceContext(unitName = PERSISTENCE_UNIT)
	private @Nullable EntityManager entityManager;

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 */
	public ActionRepository() {
		super(Action.class, Long.class);
	}

	/**
	 * Encapsulates deterministic data access for `init` so callers do not duplicate query logic across services.
	 */
	@PostConstruct
	public void init() {
		initialize(entityManager);
	}

	/**
	 * Encapsulates deterministic data access for `selectPages` so callers do not duplicate query logic across services.
	 *
	 * @param appName input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	public List<String> selectPages(String appName) {
		assertInitialized();
		return queryFactory
				.selectDistinct(qAction.page)
				.from(qAction)
				.where(qAction.appName.eq(appName))
				.orderBy(qAction.page.asc())
				.fetch();
	}

	/**
	 * Resolves which action IDs currently exist in storage so callers can validate
	 * relation inputs with one deterministic query.
	 */
	public Set<Long> findExistingIds(Collection<Long> actionIds) {
		assertInitialized();
		Assert.notNull(actionIds, "actionIds must not be null");
		if (actionIds.isEmpty()) {
			return Set.of();
		}

		return new HashSet<>(queryFactory
				.select(qAction.id)
				.from(qAction)
				.where(qAction.id.in(actionIds))
				.fetch());
	}
}

