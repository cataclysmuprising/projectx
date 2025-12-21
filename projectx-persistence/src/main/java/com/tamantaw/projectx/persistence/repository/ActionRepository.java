package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.QAction;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class ActionRepository
		extends AbstractRepositoryImpl<
		Action,
		QAction,
		ActionCriteria,
		Long> {

	private static final QAction qAction = QAction.action;

	public ActionRepository(
			@Qualifier(EM_FACTORY) EntityManager entityManager) {

		super(Action.class, entityManager);
	}

	public List<String> selectPages(String appName) {
		return queryFactory.selectDistinct(qAction.page).from(qAction).where(qAction.appName.eq(appName)).fetch();
	}
}