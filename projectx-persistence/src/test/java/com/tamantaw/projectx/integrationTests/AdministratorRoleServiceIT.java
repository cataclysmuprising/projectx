package com.tamantaw.projectx.integrationTests;

import com.tamantaw.projectx.CommonTestBase;
import com.tamantaw.projectx.persistence.criteria.AdministratorRoleCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.dto.AdministratorRoleDTO;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.AdministratorRole;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.AdministratorRoleService;
import com.tamantaw.projectx.persistence.service.AdministratorService;
import com.tamantaw.projectx.persistence.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class AdministratorRoleServiceIT extends CommonTestBase {

	@Autowired
	private AdministratorRoleService administratorRoleService;

	@Autowired
	private AdministratorService administratorService;

	@Autowired
	private RoleService roleService;

	@Test
	public void findById_existingAdministratorRole() throws Exception {
		Optional<AdministratorRoleDTO> result = administratorRoleService.findById(1L);

		assertTrue(result.isPresent());
		AdministratorRole entity = entityManager.find(AdministratorRole.class, result.get().getId());
		assertNotNull(entity);
		assertEquals(entity.getAdministrator().getId(), 1L);
		assertEquals(entity.getRole().getId(), 1L);
	}

	@Test
	public void findAll_byRoleId() throws Exception {
		AdministratorRoleCriteria criteria = new AdministratorRoleCriteria();
		criteria.setRoleId(1L);

		List<AdministratorRoleDTO> mappings = administratorRoleService.findAll(criteria);

		assertFalse(mappings.isEmpty());
		assertTrue(
				mappings.stream().allMatch(m -> {
					AdministratorRole entity = entityManager.find(AdministratorRole.class, m.getId());
					return entity != null && entity.getRole().getId().equals(1L);
				})
		);
	}

	@Test
	public void create_persistsAdministratorRole() throws ConsistencyViolationException, PersistenceException {
		AdministratorDTO newAdmin = createAdministrator("link-admin@example.com");
		RoleDTO role = roleService.findById(2L).orElseThrow();

		AdministratorDTO adminRef = new AdministratorDTO();
		adminRef.setId(newAdmin.getId());

		AdministratorRoleDTO dto = new AdministratorRoleDTO();
		dto.setAdministrator(adminRef);

		RoleDTO roleRef = new RoleDTO();
		roleRef.setId(role.getId());
		dto.setRole(roleRef);

		AdministratorRoleDTO saved = administratorRoleService.create(dto, TEST_CREATE_USER_ID);

		assertNotNull(saved.getId());
		assertEquals(saved.getAdministrator().getId(), newAdmin.getId());
		assertEquals(saved.getRole().getId(), role.getId());
	}

	@Test
	public void delete_removesAdministratorRole() throws ConsistencyViolationException, PersistenceException {
		AdministratorDTO newAdmin = createAdministrator("unlink-admin@example.com");
		RoleDTO role = roleService.findById(1L).orElseThrow();

		AdministratorDTO adminRef = new AdministratorDTO();
		adminRef.setId(newAdmin.getId());

		RoleDTO roleRef = new RoleDTO();
		roleRef.setId(role.getId());

		AdministratorRoleDTO dto = new AdministratorRoleDTO();
		dto.setAdministrator(adminRef);
		dto.setRole(roleRef);

		AdministratorRoleDTO saved = administratorRoleService.create(dto, TEST_CREATE_USER_ID);

		AdministratorRoleCriteria criteria = new AdministratorRoleCriteria();
		criteria.setAdministratorId(newAdmin.getId());

		long deleted = administratorRoleService.delete(criteria);

		assertEquals(deleted, 1L);

		entityManager.flush();
		entityManager.clear();

		assertNull(entityManager.find(AdministratorRole.class, saved.getId()));
	}

	private AdministratorDTO createAdministrator(String loginId)
			throws ConsistencyViolationException, PersistenceException {

		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("Admin for Link " + loginId);
		dto.setLoginId(loginId);
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		return administratorService.create(dto, TEST_CREATE_USER_ID);
	}
}
