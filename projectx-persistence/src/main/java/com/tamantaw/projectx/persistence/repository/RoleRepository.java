package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.entity.QRole;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

	@PersistenceContext(unitName = EM_FACTORY)
	private EntityManager entityManager;

	public RoleRepository() {
		super(Role.class, Long.class);
	}

	@PostConstruct
	public void init() {
		initialize(entityManager);
	}
}
