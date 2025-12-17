package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.AdministratorRoleCriteria;
import com.tamantaw.projectx.persistence.entity.AdministratorRole;
import com.tamantaw.projectx.persistence.entity.QAdministratorRole;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class AdministratorRoleRepository
		extends AbstractRepositoryImpl<
		AdministratorRole,
		QAdministratorRole,
		AdministratorRoleCriteria,
		Long> {

	public AdministratorRoleRepository(
			@Qualifier(EM_FACTORY) EntityManager entityManager) {

		super(AdministratorRole.class, entityManager);
	}
}

