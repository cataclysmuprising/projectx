package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.RoleActionCriteria;
import com.tamantaw.projectx.persistence.entity.QRoleAction;
import com.tamantaw.projectx.persistence.entity.RoleAction;
import com.tamantaw.projectx.persistence.exception.BusinessException;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class RoleActionRepository
		extends AbstractRepositoryImpl<
		RoleAction,
		QRoleAction,
		RoleActionCriteria,
		Long> {

	private final QRoleAction qEntity = QRoleAction.roleAction;

	public RoleActionRepository(
			@Qualifier(EM_FACTORY) EntityManager entityManager) {

		super(RoleAction.class, entityManager);
	}

	public List<Long> findActionIdsByRoleId(Long roleId) throws BusinessException {
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
