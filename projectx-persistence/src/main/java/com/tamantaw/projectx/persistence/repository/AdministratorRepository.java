package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.QAdministrator;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
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

	public AdministratorRepository(@Qualifier(EM_FACTORY) EntityManager entityManager) {
		super(Administrator.class, Long.class, entityManager);
	}
}


