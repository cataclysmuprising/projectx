package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.RoleActionCriteria;
import com.tamantaw.projectx.persistence.entity.QRoleAction;
import com.tamantaw.projectx.persistence.entity.RoleAction;
import com.tamantaw.projectx.persistence.exception.BusinessException;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class RoleActionRepository
		extends AbstractRepositoryImpl<
		Long,
		RoleAction,
		QRoleAction,
		RoleActionCriteria
		> {

	private final QRoleAction qEntity = QRoleAction.roleAction;

	@PersistenceContext(unitName = EM_FACTORY)
	private EntityManager entityManager;

	public RoleActionRepository() {
		super(RoleAction.class, Long.class);
	}

	@PostConstruct
	public void init() {
		initialize(entityManager);
	}

	public List<Long> findActionIdsByRoleId(Long roleId) throws BusinessException {
		assertInitialized();
		try {
			QRoleAction roleAction = QRoleAction.roleAction;
			//@formatter:off
			return queryFactory.select(roleAction.action.id)
					.from(roleAction)
					.where(roleAction.role.id.eq(roleId))
					.fetch();
		}
		catch (Exception e) {
			throw new BusinessException(e.getMessage(),e);
		}
	}
}
