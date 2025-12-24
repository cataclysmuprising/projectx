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

	// ----------------------------------------------------------------------
	// FIND
	// ----------------------------------------------------------------------

	@Test
	public void findById_existingAdministratorRole() throws Exception {

		Optional<AdministratorRoleDTO> result =
				administratorRoleService.findById(1L);

		assertTrue(result.isPresent());

		AdministratorRoleDTO dto = result.get();

		assertEquals(dto.getAdministratorId(), 1L);
		assertEquals(dto.getRoleId(), 1L);
	}

	@Test
	public void findAll_byRoleId() throws Exception {

		AdministratorRoleCriteria criteria = new AdministratorRoleCriteria();
		criteria.setRoleId(1L);

		List<AdministratorRoleDTO> mappings =
				administratorRoleService.findAll(criteria);

		assertFalse(mappings.isEmpty());

		assertTrue(
				mappings.stream()
						.allMatch(m -> m.getRoleId().equals(1L))
		);
	}

	// ----------------------------------------------------------------------
	// CREATE
	// ----------------------------------------------------------------------

	@Test
	public void create_persistsAdministratorRole()
			throws ConsistencyViolationException, PersistenceException {

		AdministratorDTO newAdmin =
				createAdministrator("link-admin@example.com");

		RoleDTO role =
				roleService.findById(2L).orElseThrow();

		AdministratorRoleDTO dto = new AdministratorRoleDTO();
		dto.setAdministratorId(newAdmin.getId());
		dto.setRoleId(role.getId());

		AdministratorRoleDTO saved =
				administratorRoleService.create(dto, TEST_CREATE_USER_ID);

		assertNotNull(saved.getId());
		assertEquals(newAdmin.getId(), saved.getAdministratorId());
		assertEquals(role.getId(), saved.getRoleId());
		assertEquals(saved.getCreatedBy(), TEST_CREATE_USER_ID);
	}

	// ----------------------------------------------------------------------
	// UPDATE
	// ----------------------------------------------------------------------

	@Test
	public void update_withDto_changesRoleAssociation() throws Exception {

		AdministratorDTO newAdmin =
				createAdministrator("dto-link-admin@example.com");

		RoleDTO initialRole =
				roleService.findById(1L).orElseThrow();

		RoleDTO updatedRole =
				roleService.findById(2L).orElseThrow();

		AdministratorRoleDTO createDto = new AdministratorRoleDTO();
		createDto.setAdministratorId(newAdmin.getId());
		createDto.setRoleId(initialRole.getId());

		AdministratorRoleDTO saved =
				administratorRoleService.create(createDto, TEST_CREATE_USER_ID);

		AdministratorRoleDTO updateDto = new AdministratorRoleDTO();
		updateDto.setId(saved.getId());
		updateDto.setRoleId(updatedRole.getId());

		AdministratorRoleDTO updated =
				administratorRoleService.update(updateDto, TEST_UPDATE_USER_ID);

		assertEquals(updatedRole.getId(), updated.getRoleId());
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	// ----------------------------------------------------------------------
	// DELETE
	// ----------------------------------------------------------------------

	@Test
	public void delete_removesAdministratorRole()
			throws ConsistencyViolationException, PersistenceException {

		AdministratorDTO newAdmin =
				createAdministrator("unlink-admin@example.com");

		RoleDTO role =
				roleService.findById(1L).orElseThrow();

		AdministratorRoleDTO dto = new AdministratorRoleDTO();
		dto.setAdministratorId(newAdmin.getId());
		dto.setRoleId(role.getId());

		AdministratorRoleDTO saved =
				administratorRoleService.create(dto, TEST_CREATE_USER_ID);

		AdministratorRoleCriteria criteria = new AdministratorRoleCriteria();
		criteria.setAdministratorId(newAdmin.getId());

		long deleted =
				administratorRoleService.delete(criteria);

		assertEquals(deleted, 1L);

		entityManager.flush();
		entityManager.clear();

		assertNull(
				entityManager.find(AdministratorRole.class, saved.getId())
		);
	}

	@Test
	public void deleteById_removesAdministratorRoleByIdentifier() throws Exception {

		AdministratorDTO newAdmin =
				createAdministrator("delete-by-id-admin@example.com");

		RoleDTO role =
				roleService.findById(1L).orElseThrow();

		AdministratorRoleDTO dto = new AdministratorRoleDTO();
		dto.setAdministratorId(newAdmin.getId());
		dto.setRoleId(role.getId());

		AdministratorRoleDTO saved =
				administratorRoleService.create(dto, TEST_CREATE_USER_ID);

		boolean deleted =
				administratorRoleService.deleteById(saved.getId());

		assertTrue(deleted);
		assertNull(
				entityManager.find(AdministratorRole.class, saved.getId())
		);
	}

	// ----------------------------------------------------------------------
	// HELPERS
	// ----------------------------------------------------------------------

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
