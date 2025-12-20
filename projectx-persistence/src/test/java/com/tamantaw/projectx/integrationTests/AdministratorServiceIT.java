package com.tamantaw.projectx.integrationTests;

import com.tamantaw.projectx.CommonTestBase;
import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.QAdministrator;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.repository.base.UpdateSpec;
import com.tamantaw.projectx.persistence.service.AdministratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class AdministratorServiceIT extends CommonTestBase {

	@Autowired
	private AdministratorService administratorService;

	@Test
	public void findById_existingAdministrator() throws Exception {
		Optional<AdministratorDTO> result = administratorService.findById(1L);

		assertTrue(result.isPresent());
		assertEquals(result.get().getLoginId(), "tha@superuser");
		assertEquals(result.get().getStatus(), Administrator.Status.ACTIVE);
	}

	@Test
	public void findOne_byLoginId() throws Exception {
		AdministratorCriteria criteria = new AdministratorCriteria();
		criteria.setLoginId("zeyar@superuser");

		Optional<AdministratorDTO> result = administratorService.findOne(criteria);

		assertTrue(result.isPresent());
		assertEquals(result.get().getName(), "Zeyar Phyo Maung");
	}

	@Test
	public void findAll_byRoleCriteria() throws Exception {
		RoleCriteria roleCriteria = new RoleCriteria();
		roleCriteria.setName("SUPER-USER");

		AdministratorCriteria criteria = new AdministratorCriteria();
		criteria.setRole(roleCriteria);

		List<AdministratorDTO> administrators = administratorService.findAll(criteria);

		assertFalse(administrators.isEmpty());
		assertTrue(
				administrators.stream()
						.allMatch(a -> a.getLoginId().contains("@superuser"))
		);
	}

	@Test
	public void create_persistsAdministrator() throws ConsistencyViolationException, PersistenceException {
		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("Integration Admin");
		dto.setLoginId("integration-admin@example.com");
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		Administrator saved = administratorService.create(dto, TEST_CREATE_USER_ID);

		assertNotNull(saved.getId());
		assertEquals(saved.getCreatedBy(), TEST_CREATE_USER_ID);
		assertEquals(saved.getUpdatedBy(), TEST_CREATE_USER_ID);
	}

	@Test
	public void update_updatesStatus() throws Exception {
		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("Updatable Admin");
		dto.setLoginId("update-admin@example.com");
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		administratorService.create(dto, TEST_CREATE_USER_ID);

		AdministratorCriteria criteria = new AdministratorCriteria();
		criteria.setLoginId("update-admin@example.com");

		UpdateSpec<Administrator> spec = (update, root) ->
				update.set(QAdministrator.administrator.status, Administrator.Status.SUSPENDED);

		long affected = administratorService.update(spec, criteria, TEST_UPDATE_USER_ID);

		assertEquals(affected, 1L);

		Administrator updated = entityManager
				.createQuery(
						"select a from Administrator a where a.loginId = :login",
						Administrator.class
				)
				.setParameter("login", "update-admin@example.com")
				.getSingleResult();

		assertEquals(updated.getStatus(), Administrator.Status.SUSPENDED);
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	@Test
	public void delete_removesAdministrator() throws ConsistencyViolationException, PersistenceException {
		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("Deletable Admin");
		dto.setLoginId("delete-admin@example.com");
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		Administrator saved = administratorService.create(dto, TEST_CREATE_USER_ID);

		AdministratorCriteria criteria = new AdministratorCriteria();
		criteria.setLoginId("delete-admin@example.com");

		long deleted = administratorService.delete(criteria);

		assertEquals(deleted, 1L);

		entityManager.flush();
		entityManager.clear();

		assertNull(entityManager.find(Administrator.class, saved.getId()));
	}
}
