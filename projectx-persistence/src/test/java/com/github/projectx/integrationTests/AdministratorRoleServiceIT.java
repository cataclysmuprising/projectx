package com.github.projectx.integrationTests;

import com.github.projectx.CommonTestBase;
import com.github.projectx.persistence.criteria.rba.AdministratorRoleCriteria;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.rba.AdministratorDTO;
import com.github.projectx.persistence.dto.rba.AdministratorRoleDTO;
import com.github.projectx.persistence.dto.rba.RoleDTO;
import com.github.projectx.persistence.entity.rba.Administrator;
import com.github.projectx.persistence.entity.rba.QAdministratorRole;
import com.github.projectx.persistence.exception.BusinessException;
import com.github.projectx.persistence.exception.ConsistencyViolationException;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.service.rba.AdministratorRoleService;
import com.github.projectx.persistence.service.rba.AdministratorService;
import com.github.projectx.persistence.service.rba.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class AdministratorRoleServiceIT extends CommonTestBase {

	private static final String FETCH_GRAPH = "AdministratorRole(administrator,role)";

	@Autowired
	private AdministratorRoleService administratorRoleService;

	@Autowired
	private AdministratorService administratorService;

	@Autowired
	private RoleService roleService;

	// ----------------------------------------------------------------------
	// FIND
	// ----------------------------------------------------------------------

	@Test(groups = "fetch")
	public void findById_existingAdministratorRole() throws Exception {

		Optional<AdministratorRoleDTO> result =
				administratorRoleService.findById(1L);

		assertTrue(result.isPresent());

		AdministratorRoleDTO dto = result.get();

		assertEquals(dto.getAdministratorId(), 1L);
		assertEquals(dto.getRoleId(), 1L);
	}

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
	public void findByPaging_basicPagination() throws Exception {
		AdministratorRoleCriteria criteria = new AdministratorRoleCriteria();
		criteria.setRoleId(1L);
		criteria.setLimit(1);
		criteria.setOffset(0);

		PaginatedResult<AdministratorRoleDTO> page =
				administratorRoleService.findByPaging(criteria);

		assertNotNull(page);
		assertTrue(page.getTotalElements() >= 1);
		assertEquals(page.getPageSize(), 1);
		assertEquals(page.getPageNumber(), 0);
		assertEquals(page.getData().size(), 1);
	}

	@Test(groups = "fetch")
	public void findAll_withFetchGraph_includesAdministratorAndRole() throws Exception {
		AdministratorRoleCriteria criteria = new AdministratorRoleCriteria();
		criteria.setRoleId(1L);

		List<AdministratorRoleDTO> mappings =
				administratorRoleService.findAll(criteria, FETCH_GRAPH);

		assertFalse(mappings.isEmpty());

		AdministratorRoleDTO dto = mappings.getFirst();
		assertNotNull(dto.getAdministrator());
		assertNotNull(dto.getRole());
	}

	// ----------------------------------------------------------------------
	// CREATE
	// ----------------------------------------------------------------------

	@Test(groups = "fetch")
	public void create_persistsAdministratorRole()
			throws ConsistencyViolationException, PersistenceException, BusinessException {

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

	@Test(groups = "fetch")
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
				administratorRoleService.update(updateDto, QAdministratorRole.administratorRole, TEST_UPDATE_USER_ID);

		assertEquals(updatedRole.getId(), updated.getRoleId());
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	// ----------------------------------------------------------------------
	// DELETE
	// ----------------------------------------------------------------------

	@Test(groups = "fetch")
	public void delete_removesAdministratorRole()
			throws ConsistencyViolationException, PersistenceException, BusinessException {

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

		assertTrue(
				administratorRoleService.findById(saved.getId()).isEmpty()
		);
	}

	@Test(groups = "fetch")
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
		assertTrue(
				administratorRoleService.findById(saved.getId()).isEmpty()
		);
	}

	// ----------------------------------------------------------------------
	// HELPERS
	// ----------------------------------------------------------------------

	private AdministratorDTO createAdministrator(String loginId)
			throws ConsistencyViolationException, PersistenceException, BusinessException {

		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("Admin for Link " + loginId);
		dto.setLoginId(loginId);
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		return administratorService.create(dto, TEST_CREATE_USER_ID);
	}
}


