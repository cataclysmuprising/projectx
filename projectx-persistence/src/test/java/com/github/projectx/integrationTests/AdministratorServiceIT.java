package com.github.projectx.integrationTests;

import com.github.projectx.CommonTestBase;
import com.github.projectx.persistence.criteria.rba.AdministratorCriteria;
import com.github.projectx.persistence.criteria.rba.AdministratorRoleCriteria;
import com.github.projectx.persistence.criteria.rba.RoleCriteria;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.rba.AdministratorDTO;
import com.github.projectx.persistence.dto.rba.AdministratorRoleDTO;
import com.github.projectx.persistence.entity.rba.Administrator;
import com.github.projectx.persistence.entity.rba.QAdministrator;
import com.github.projectx.persistence.service.rba.AdministratorRoleService;
import com.github.projectx.persistence.service.rba.AdministratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.*;

public class AdministratorServiceIT extends CommonTestBase {

	private static final String FETCH_GRAPH = "Administrator(administratorRoles(role))";

	@Autowired
	private AdministratorService administratorService;

	@Autowired
	private AdministratorRoleService administratorRoleService;

	@Test(groups = "fetch")
	public void create_withRoles_persistsAdministratorAndRoles() throws Exception {
		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("Admin With Roles");
		dto.setLoginId("admin-with-roles@example.com");
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		Set<Long> roleIds = Set.of(1L, 2L);

		AdministratorDTO saved =
				administratorService.create(dto, roleIds, TEST_CREATE_USER_ID);

		AdministratorRoleCriteria criteria = new AdministratorRoleCriteria();
		criteria.setAdministratorId(saved.getId());

		List<AdministratorRoleDTO> roles =
				administratorRoleService.findAll(criteria, "AdministratorRole(role)");

		assertNotNull(saved.getId());
		assertEquals(roles.size(), 2);
		assertEquals(saved.getCreatedBy(), TEST_CREATE_USER_ID);
		assertEquals(saved.getUpdatedBy(), TEST_CREATE_USER_ID);

		assertTrue(
				roles.stream()
						.map(ar -> ar.getRole().getId())
						.collect(Collectors.toSet())
						.containsAll(List.of(1L, 2L))
		);

		assertTrue(
				roles.stream().allMatch(ar ->
						ar.getCreatedBy() == TEST_CREATE_USER_ID &&
								ar.getUpdatedBy() == TEST_CREATE_USER_ID
				)
		);
	}

	@Test(groups = "fetch")
	public void updateAdministratorAndRoles_replacesRoles() throws Exception {

		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("Admin For Update");
		dto.setLoginId("update-roles-admin@example.com");
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		AdministratorDTO saved =
				administratorService.create(
						dto,
						Set.of(1L),
						TEST_CREATE_USER_ID
				);

		AdministratorDTO updateDto = new AdministratorDTO();
		updateDto.setId(saved.getId());
		updateDto.setName("Admin Updated");
		updateDto.setStatus(Administrator.Status.ACTIVE);

		administratorService.updateAdministratorAndRoles(
				updateDto,
				Set.of(2L, 1L),
				TEST_UPDATE_USER_ID
		);

		AdministratorRoleCriteria criteria = new AdministratorRoleCriteria();
		criteria.setAdministratorId(saved.getId());

		List<AdministratorRoleDTO> roles =
				administratorRoleService.findAll(criteria, "AdministratorRole(role)");

		assertEquals(roles.size(), 2);
		assertTrue(
				roles.stream()
						.map(ar -> ar.getRole().getId())
						.collect(Collectors.toSet())
						.containsAll(List.of(1L, 2L))
		);
	}

	@Test(groups = "fetch")
	public void findById_existingAdministrator() throws Exception {
		Optional<AdministratorDTO> result =
				administratorService.findById(1L);

		assertTrue(result.isPresent());
		assertEquals(result.get().getLoginId(), "alice@superuser");
		assertEquals(result.get().getStatus(), Administrator.Status.ACTIVE);
	}

	@Test(groups = "fetch")
	public void findOne_byLoginId() throws Exception {
		AdministratorCriteria criteria = new AdministratorCriteria();
		criteria.setLoginId("bob@superuser");

		Optional<AdministratorDTO> result =
				administratorService.findOne(criteria);

		assertTrue(result.isPresent());
		assertEquals(result.get().getName(), "Bob Nguyen");
	}

	@Test(groups = "fetch")
	public void findAll_byRoleCriteria() throws Exception {
		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("Role Criteria Admin");
		dto.setLoginId("role-criteria-admin+" + System.nanoTime() + "@example.com");
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		AdministratorDTO saved =
				administratorService.create(
						dto,
						Set.of(1L),
						TEST_CREATE_USER_ID
				);

		RoleCriteria roleCriteria = new RoleCriteria();
		roleCriteria.setId(1L);

		AdministratorCriteria criteria = new AdministratorCriteria();
		criteria.setRole(roleCriteria);

		List<AdministratorDTO> administrators =
				administratorService.findAll(criteria);

		assertFalse(administrators.isEmpty());
		assertTrue(
				administrators.stream()
						.anyMatch(a -> a.getId().equals(saved.getId()))
		);
	}

	@Test(groups = "fetch")
	public void findByPaging_basicPagination() throws Exception {
		AdministratorCriteria criteria = new AdministratorCriteria();
		criteria.setStatus(Administrator.Status.ACTIVE);
		criteria.setLimit(1);
		criteria.setOffset(0);

		PaginatedResult<AdministratorDTO> page =
				administratorService.findByPaging(criteria);

		assertNotNull(page);
		assertTrue(page.getTotalElements() >= 1);
		assertEquals(page.getPageSize(), 1);
		assertEquals(page.getPageNumber(), 0);
		assertEquals(page.getData().size(), 1);
	}

	@Test(groups = "fetch")
	public void findAll_withFetchGraph_includesRoles() throws Exception {
		AdministratorCriteria criteria = new AdministratorCriteria();
		criteria.setLoginId("alice@superuser");

		List<AdministratorDTO> admins =
				administratorService.findAll(criteria, FETCH_GRAPH);

		assertFalse(admins.isEmpty());

		AdministratorDTO dto = admins.getFirst();
		assertNotNull(dto.getRoles());
		assertFalse(dto.getRoles().isEmpty());
	}

	@Test(groups = "fetch")
	public void create_persistsAdministrator() throws Exception {
		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("Integration Admin");
		dto.setLoginId("integration-admin@example.com");
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		AdministratorDTO saved =
				administratorService.create(dto, TEST_CREATE_USER_ID);

		assertNotNull(saved.getId());
		assertEquals(saved.getCreatedBy(), TEST_CREATE_USER_ID);
		assertEquals(saved.getUpdatedBy(), TEST_CREATE_USER_ID);
	}

	@Test(groups = "fetch")
	public void update_withDto_updatesFieldsAndAudit() throws Exception {
		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("Dto Update Admin");
		dto.setLoginId("dto-update-admin@example.com");
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		AdministratorDTO saved =
				administratorService.create(dto, TEST_CREATE_USER_ID);

		AdministratorDTO updateDto = new AdministratorDTO();
		updateDto.setId(saved.getId());
		updateDto.setName("Updated DTO Admin");
		updateDto.setStatus(Administrator.Status.SUSPENDED);

		AdministratorDTO updated =
				administratorService.update(updateDto, QAdministrator.administrator, TEST_UPDATE_USER_ID);

		assertEquals(updated.getName(), "Updated DTO Admin");
		assertEquals(updated.getStatus(), Administrator.Status.SUSPENDED);
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	@Test(groups = "fetch")
	public void deleteById_removesAdministratorByIdentifier() throws Exception {
		AdministratorDTO dto = new AdministratorDTO();
		dto.setName("DeleteById Admin");
		dto.setLoginId("delete-by-id-admin@example.com");
		dto.setPassword("secret");
		dto.setStatus(Administrator.Status.ACTIVE);

		AdministratorDTO saved =
				administratorService.create(dto, TEST_CREATE_USER_ID);

		boolean deleted =
				administratorService.deleteById(saved.getId());

		assertTrue(deleted);
		assertTrue(administratorService.findById(saved.getId()).isEmpty());
	}
}


