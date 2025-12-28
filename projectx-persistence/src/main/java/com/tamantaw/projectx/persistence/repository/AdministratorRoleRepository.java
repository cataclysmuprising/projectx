package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.AdministratorRoleCriteria;
import com.tamantaw.projectx.persistence.entity.AdministratorRole;
import com.tamantaw.projectx.persistence.entity.QAdministratorRole;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class AdministratorRoleRepository
		extends AbstractRepositoryImpl<
		Long,
		AdministratorRole,
		QAdministratorRole,
		AdministratorRoleCriteria
		> {

	@PersistenceContext(unitName = EM_FACTORY)
	private EntityManager entityManager;

	public AdministratorRoleRepository() {
		super(AdministratorRole.class, Long.class);
	}

	@PostConstruct
	public void init() {
		initialize(entityManager);
	}
}
