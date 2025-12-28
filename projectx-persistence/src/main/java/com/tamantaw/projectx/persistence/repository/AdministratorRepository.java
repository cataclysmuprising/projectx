package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.QAdministrator;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class AdministratorRepository
		extends AbstractRepositoryImpl<
		Long,
		Administrator,
		QAdministrator,
		AdministratorCriteria
		> {

	@PersistenceContext(unitName = EM_FACTORY)
	private EntityManager entityManager;

	public AdministratorRepository() {
		super(Administrator.class, Long.class);
	}

	@PostConstruct
	public void init() {
		initialize(entityManager);
	}
}

