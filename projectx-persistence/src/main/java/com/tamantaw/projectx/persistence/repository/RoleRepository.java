package com.tamantaw.projectx.persistence.repository;

import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.entity.QRole;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext.EM_FACTORY;

@Repository
public class RoleRepository
		extends AbstractRepositoryImpl<
		Role,
		QRole,
		RoleCriteria,
		Long> {

	private final QRole qEntity = QRole.role;

	public RoleRepository(
			@Qualifier(EM_FACTORY) EntityManager entityManager) {

		super(Role.class, entityManager);
	}

	public List<String> selectRolesByActionURL(String actionUrl, String appName) {
		try {

			String query = "SELECT DISTINCT " +
					" rol.name AS rol_name " +
					" FROM rpt_action act " +
					" INNER JOIN rpt_role_x_action ra ON ra.action_id = act.id " +
					" INNER JOIN rpt_role rol ON rol.id = ra.role_id " +
					" WHERE act.app_name = :appName " +
					" AND :actionUrl ~ act.url ";

			Query appliedQuery = entityManager.createNativeQuery(query)
					.setParameter("actionUrl", actionUrl)
					.setParameter("appName", appName);

			return appliedQuery.getResultList();
		}
		catch (Exception ex) {
			throw new IncorrectResultSizeDataAccessException(ex.getMessage(), 1, ex);
		}
	}
}
