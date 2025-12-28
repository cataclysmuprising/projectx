package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.QAction;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class ActionRepository
		extends AbstractRepositoryImpl<
		Long,
		Action,
		QAction,
		ActionCriteria
		> {

	private static final QAction qAction = QAction.action;

	@PersistenceContext(unitName = EM_FACTORY)
	private EntityManager entityManager;

	public ActionRepository() {
		super(Action.class, Long.class);
	}

	@PostConstruct
	public void init() {
		initialize(entityManager);
	}

	public List<String> selectPages(String appName) {
		assertInitialized();
		return queryFactory.selectDistinct(qAction.page).from(qAction).where(qAction.appName.eq(appName)).fetch();
	}
}
