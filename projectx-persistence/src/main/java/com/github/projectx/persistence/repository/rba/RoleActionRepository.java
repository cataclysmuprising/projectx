package com.github.projectx.persistence.repository.rba;

import com.github.projectx.persistence.criteria.rba.RoleActionCriteria;
import com.github.projectx.persistence.entity.rba.QAction;
import com.github.projectx.persistence.entity.rba.QRole;
import com.github.projectx.persistence.entity.rba.QRoleAction;
import com.github.projectx.persistence.entity.rba.RoleAction;
import com.github.projectx.persistence.exception.BusinessException;
import com.github.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.util.List;

import static com.github.projectx.persistence.config.PersistenceContextNames.PERSISTENCE_UNIT;

@Repository
public class RoleActionRepository
		extends AbstractRepositoryImpl<
		Long,
		RoleAction,
		QRoleAction,
		RoleActionCriteria
		> {

	private static final QRoleAction qRoleAction = QRoleAction.roleAction;
	private static final QRole qRole = QRole.role;
	private static final QAction qAction = QAction.action;

	@PersistenceContext(unitName = PERSISTENCE_UNIT)
	private @Nullable EntityManager entityManager;

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 */
	public RoleActionRepository() {
		super(RoleAction.class, Long.class);
	}

	/**
	 * Encapsulates deterministic data access for `init` so callers do not duplicate query logic across services.
	 */
	@PostConstruct
	public void init() {
		initialize(entityManager);
	}

	/**
	 * Encapsulates deterministic data access for `findActionIdsByRoleId` so callers do not duplicate query logic across services.
	 *
	 * @param roleId input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	public List<Long> findActionIdsByRoleId(Long roleId) throws BusinessException {
		assertInitialized();
		try {
			//@formatter:off
			return queryFactory.select(qRoleAction.action.id)
					.from(qRoleAction)
					.where(qRoleAction.role.id.eq(roleId))
					.fetch();
		}
		catch (RuntimeException e) {
			throw new BusinessException(e.getMessage(),e);
		}
	}

	public List<RoleNameBindingRow> findRoleNameBindingsByAppName(String appName) {
		assertInitialized();
		Assert.hasText(appName, "appName must not be blank");

		return queryFactory
				.select(
						qRoleAction.actionId,
						qRole.name
				)
				.from(qRoleAction)
				.join(qRoleAction.role, qRole)
				.join(qRoleAction.action, qAction)
				.where(
						qRole.appName.eq(appName),
						qAction.appName.eq(appName)
				)
				.orderBy(
						qRoleAction.actionId.asc(),
						qRole.name.asc()
				)
				.fetch()
				.stream()
				.map(tuple -> new RoleNameBindingRow(
						tuple.get(qRoleAction.actionId),
						tuple.get(qRole.name)
				))
				.toList();
	}

	public record RoleNameBindingRow(
			Long actionId,
			String roleName
	) {
	}
}

