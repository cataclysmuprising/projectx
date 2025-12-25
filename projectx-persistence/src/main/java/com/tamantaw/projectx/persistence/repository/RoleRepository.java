package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.entity.QRole;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class RoleRepository
		extends AbstractRepositoryImpl<
		Long,
		Role,
		QRole,
		RoleCriteria
		> {

	private final QRole qEntity = QRole.role;

	public RoleRepository(
			@Qualifier(EM_FACTORY) EntityManager entityManager) {

		super(Role.class, Long.class, entityManager);
	}
}
