package com.tamantaw.projectx.integrationTests;

import com.tamantaw.projectx.CommonTestBase;
import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.criteria.AdministratorRoleCriteria;
import com.tamantaw.projectx.persistence.criteria.RoleActionCriteria;
import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.dto.AdministratorRoleDTO;
import com.tamantaw.projectx.persistence.dto.RoleActionDTO;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.dto.base.PaginatedResult;
import com.tamantaw.projectx.persistence.entity.QRole;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.repository.base.UpdateSpec;
import com.tamantaw.projectx.persistence.service.AdministratorRoleService;
import com.tamantaw.projectx.persistence.service.RoleActionService;
import com.tamantaw.projectx.persistence.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.*;

public class RoleServiceIT extends CommonTestBase {

	@Autowired
	private RoleService roleService;

	@Autowired
	private RoleActionService roleActionService;

	@Autowired
	private AdministratorRoleService administratorRoleService;

	@Test
	public void updateRoleAndRelations() throws Exception {
		// ------------------------------------------------------------
		// Create Role with initial action + administrator
		// ------------------------------------------------------------
		RoleDTO dto = new RoleDTO();
		dto.setAppName("projectx");
		dto.setName("ROLE_FOR_UPDATE");
		dto.setRoleType(Role.RoleType.CUSTOM);

		RoleDTO saved = roleService.create(
				dto,
				Set.of(10021L),          // initial action
				Set.of(1L),          // initial administrator
				TEST_CREATE_USER_ID
		);

		dto.setId(saved.getId());

		// ------------------------------------------------------------
		// Update role with new action + administrator set
		// ------------------------------------------------------------
		roleService.updateRoleAndRelations(
				dto,
				Set.of(10021L, 10022L),  // updated actions
				Set.of(1L, 2L),  // updated administrators
				TEST_UPDATE_USER_ID
		);

		// ------------------------------------------------------------
		// Verify role-actions
		// ------------------------------------------------------------
		RoleActionCriteria actionCriteria = new RoleActionCriteria();
		actionCriteria.setRoleId(saved.getId());

		List<RoleActionDTO> roleActions =
				roleActionService.findAll(actionCriteria, "RoleAction(action)");

		assertEquals(roleActions.size(), 2);

		Set<Long> actionIds = roleActions.stream()
				.map(ra -> ra.getAction().getId())
				.collect(Collectors.toSet());

		assertTrue(actionIds.containsAll(Set.of(10021L, 10022L)));

		// ------------------------------------------------------------
		// Verify role-administrators
		// ------------------------------------------------------------
		AdministratorRoleCriteria adminCriteria = new AdministratorRoleCriteria();
		adminCriteria.setRoleId(saved.getId());

		List<AdministratorRoleDTO> administratorRoles =
				administratorRoleService.findAll(
						adminCriteria,
						"AdministratorRole(administrator)"
				);

		assertEquals(administratorRoles.size(), 2);

		Set<Long> administratorIds = administratorRoles.stream()
				.map(ar -> ar.getAdministrator().getId())
				.collect(Collectors.toSet());

		assertTrue(administratorIds.containsAll(Set.of(1L, 2L)));
	}

	// ----------------------------------------------------------------------
	// FIND BY ID (seed data)
	// ----------------------------------------------------------------------

	@Test
	public void findById_existingRole() throws Exception {
		Optional<RoleDTO> result = roleService.findById(1L);

		assertTrue(result.isPresent());
		assertEquals(result.get().getName(), "SUPER-USER");
		assertEquals(result.get().getAppName(), "projectx");
		assertEquals(result.get().getRoleType(), Role.RoleType.SUPERUSER);
	}

	// ----------------------------------------------------------------------
	// FIND ONE (CRITERIA)
	// ----------------------------------------------------------------------

	@Test
	public void findOne_byName() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setName("ADMINISTRATOR");

		Optional<RoleDTO> result = roleService.findOne(criteria);

		assertTrue(result.isPresent());
		assertEquals(result.get().getName(), "ADMINISTRATOR");
		assertEquals(result.get().getAppName(), "projectx");
	}

	// ----------------------------------------------------------------------
	// FIND ALL (CRITERIA)
	// ----------------------------------------------------------------------

	@Test
	public void findAll_byAppName() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");

		List<RoleDTO> roles = roleService.findAll(criteria);

		assertTrue(roles.size() >= 2);
		assertTrue(
				roles.stream().anyMatch(r -> r.getName().equals("SUPER-USER"))
		);
	}

	// ----------------------------------------------------------------------
	// FIND ROLE WITH ACTION FILTER (JOIN TEST)
	// ----------------------------------------------------------------------

	@Test
	public void findRole_byActionCriteria() throws Exception {
		ActionCriteria actionCriteria = new ActionCriteria();
		actionCriteria.setActionName("dashboard");

		RoleCriteria roleCriteria = new RoleCriteria();
		roleCriteria.setAction(actionCriteria);

		List<RoleDTO> roles = roleService.findAll(roleCriteria);

		assertFalse(roles.isEmpty());

		assertTrue(
				roles.stream().anyMatch(r -> r.getName().equals("SUPER-USER"))
		);
	}

	// ----------------------------------------------------------------------
	// CREATE
	// ----------------------------------------------------------------------

	@Test
	public void create_persistsRole() throws Exception {
		RoleDTO dto = new RoleDTO();
		dto.setAppName("projectx");
		dto.setName("TEMP_ROLE");
		dto.setRoleType(Role.RoleType.CUSTOM);
		dto.setDescription("Temporary role for IT test");

		RoleDTO saved = roleService.create(dto, 100L);

		assertNotNull(saved.getId());
		assertEquals(saved.getName(), "TEMP_ROLE");
		assertEquals(saved.getCreatedBy(), 100L);
		assertEquals(saved.getUpdatedBy(), 100L);
	}

	@Test
	public void create_withRelations() throws Exception {
		RoleDTO dto = new RoleDTO();
		dto.setAppName("projectx");
		dto.setName("ROLE_WITH_ACTIONS");
		dto.setRoleType(Role.RoleType.CUSTOM);

		// initial relations
		Set<Long> actionIds = Set.of(10021L, 10022L);
		Set<Long> administratorIds = Set.of(1L, 2L);

		RoleDTO saved = roleService.create(
				dto,
				actionIds,
				administratorIds,
				123L
		);

		// ------------------------------------------------------------
		// Verify role-actions
		// ------------------------------------------------------------
		RoleActionCriteria actionCriteria = new RoleActionCriteria();
		actionCriteria.setRoleId(saved.getId());

		List<RoleActionDTO> roleActions =
				roleActionService.findAll(actionCriteria, "RoleAction(action)");

		assertNotNull(saved.getId());
		assertEquals(roleActions.size(), 2);
		assertEquals(saved.getCreatedBy(), 123L);
		assertEquals(saved.getUpdatedBy(), 123L);

		assertTrue(
				roleActions.stream()
						.map(RoleActionDTO::getAction)
						.map(ActionDTO::getId)
						.collect(Collectors.toSet())
						.containsAll(Set.of(10021L, 10022L))
		);

		assertTrue(
				roleActions.stream()
						.allMatch(
								ra -> ra.getCreatedBy() == 123L
										&& ra.getUpdatedBy() == 123L
						)
		);

		// ------------------------------------------------------------
		// Verify role-administrators
		// ------------------------------------------------------------
		AdministratorRoleCriteria adminCriteria = new AdministratorRoleCriteria();
		adminCriteria.setRoleId(saved.getId());

		List<AdministratorRoleDTO> administratorRoles =
				administratorRoleService.findAll(
						adminCriteria,
						"AdministratorRole(administrator)"
				);

		assertEquals(administratorRoles.size(), 2);

		assertTrue(
				administratorRoles.stream()
						.map(ar -> ar.getAdministrator().getId())
						.collect(Collectors.toSet())
						.containsAll(Set.of(1L, 2L))
		);

		assertTrue(
				administratorRoles.stream()
						.allMatch(
								ar -> ar.getCreatedBy() == 123L
										&& ar.getUpdatedBy() == 123L
						)
		);
	}

	// ----------------------------------------------------------------------
	// UPDATE
	// ----------------------------------------------------------------------

	@Test
	public void update_updatesRoleName() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setName("ADMINISTRATOR");

		UpdateSpec<Role> spec = (update, root) ->
				update.set(QRole.role.name, "ADMIN_UPDATED");

		long affected = roleService.update(spec, criteria, 200L);

		assertEquals(affected, 1L);

		RoleCriteria updatedCriteria = new RoleCriteria();
		updatedCriteria.setName("ADMIN_UPDATED");

		RoleDTO updated = roleService.findOne(updatedCriteria).orElseThrow();

		assertEquals(updated.getUpdatedBy(), 200L);
	}

	@Test
	public void update_withDto_updatesRoleFieldsAndAudit() throws Exception {
		RoleDTO dto = new RoleDTO();
		dto.setAppName("projectx");
		dto.setName("DTO_UPDATE_ROLE");
		dto.setRoleType(Role.RoleType.CUSTOM);
		dto.setDescription("Role for DTO update");

		RoleDTO saved = roleService.create(dto, TEST_CREATE_USER_ID);

		RoleDTO updateDto = new RoleDTO();
		updateDto.setId(saved.getId());
		updateDto.setName("DTO_UPDATED_ROLE");
		updateDto.setDescription("Updated via DTO update");

		RoleDTO updated = roleService.update(updateDto, TEST_UPDATE_USER_ID);

		assertEquals(updated.getName(), "DTO_UPDATED_ROLE");
		assertEquals(updated.getDescription(), "Updated via DTO update");
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	// ----------------------------------------------------------------------
	// DELETE
	// ----------------------------------------------------------------------

	@Test
	public void delete_removesRole() throws ConsistencyViolationException, PersistenceException {
		RoleDTO dto = new RoleDTO();
		dto.setAppName("projectx");
		dto.setName("DELETE_ME");
		dto.setRoleType(Role.RoleType.CUSTOM);

		RoleDTO saved = roleService.create(dto, 1L);

		RoleCriteria criteria = new RoleCriteria();
		criteria.setName("DELETE_ME");

		long deleted = roleService.delete(criteria);

		assertEquals(deleted, 1L);

		assertTrue(roleService.findById(saved.getId()).isEmpty());
	}

	@Test
	public void deleteById_removesRoleByIdentifier() throws ConsistencyViolationException, PersistenceException {
		RoleDTO dto = new RoleDTO();
		dto.setAppName("projectx");
		dto.setName("DELETE_BY_ID_ROLE");
		dto.setRoleType(Role.RoleType.CUSTOM);
		dto.setDescription("Role for deleteById test");

		RoleDTO saved = roleService.create(dto, 1L);

		boolean deleted = roleService.deleteById(saved.getId());

		assertTrue(deleted);
		assertTrue(roleService.findById(saved.getId()).isEmpty());
	}

	@Test
	public void findRole_byActionCriteria_returnsRoleWithActions() throws PersistenceException {
		// given
		ActionCriteria actionCriteria = new ActionCriteria();
		actionCriteria.setPage("Administrator");
		RoleCriteria roleCriteria = new RoleCriteria();
		roleCriteria.setAppName("projectx");
		roleCriteria.setAction(actionCriteria);
		roleCriteria.addSort(QRole.role.roleType, Sort.Direction.DESC);
		roleCriteria.addSort(QRole.role.name, Sort.Direction.ASC);

		// when
		List<RoleDTO> roles = roleService.findAll(roleCriteria, "Role(roleActions(action))");

		// then
		assertFalse(roles.isEmpty());

		RoleDTO role = roles.stream()
				.filter(r -> "SUPER-USER".equals(r.getName()))
				.findFirst()
				.orElseThrow();

		assertNotNull(role.getActions());
		assertFalse(role.getActions().isEmpty());
		showEntriesOfCollection(roles);
	}

	@Test
	public void findByPaging_basicPagination() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setLimit(1);
		criteria.setOffset(0);

		PaginatedResult<RoleDTO> page = roleService.findByPaging(criteria);

		assertNotNull(page);
		assertTrue(page.getRecordsTotal() >= 2);
		assertEquals(page.getSize(), 1);
		assertEquals(page.getNumber(), 0);
		assertEquals(page.getData().size(), 1);
	}

	@Test
	public void findByPaging_emptyPage() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setLimit(10);
		criteria.setOffset(1000);

		PaginatedResult<RoleDTO> page = roleService.findByPaging(criteria);

		assertNotNull(page);
		assertEquals(page.getNumberOfElements(), 0);
		assertTrue(page.getData().isEmpty());
	}

	@Test
	public void findByPaging_withPageNumberOnly_usesDefaultLimit() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setPageNumber(1); // 1-based

		PaginatedResult<RoleDTO> page = roleService.findByPaging(criteria);

		assertNotNull(page);
		assertTrue(page.getRecordsTotal() >= 2);
		assertEquals(page.getNumber(), 0); // Spring Page is 0-based
		assertTrue(page.getSize() > 0);    // should be DEFAULT_PAGE_SIZE (20) if you expose it
	}

	@Test
	public void findByPaging_withPageNumberAndLimit() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setPageNumber(1);
		criteria.setLimit(1);

		PaginatedResult<RoleDTO> page = roleService.findByPaging(criteria);

		assertNotNull(page);
		assertEquals(page.getNumber(), 0);
		assertEquals(page.getSize(), 1);
		assertEquals(page.getData().size(), 1);
	}

	@Test
	public void findAll_sortedByIdDesc() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.addSort(QRole.role.id, Sort.Direction.DESC);

		List<RoleDTO> roles = roleService.findAll(criteria);

		assertTrue(roles.size() >= 2);
		assertTrue(roles.get(0).getId() > roles.get(1).getId());
	}

	@Test
	public void findAll_multiSort_roleTypeAsc_nameAsc() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");

		//criteria.addSort(QRole.role.roleType, Sort.Direction.DESC);
		//criteria.addSort(QRole.role.name, Sort.Direction.ASC);

		// String based sorting
		criteria.addSortKey("roleType", Sort.Direction.DESC);
		criteria.addSortKey("name", Sort.Direction.ASC);

		List<RoleDTO> roles = roleService.findAll(criteria);

		assertTrue(roles.size() >= 2);
	}

	@Test
	public void findByPaging_withNestedActionCriteria() throws Exception {
		ActionCriteria actionCriteria = new ActionCriteria();
		actionCriteria.setPage("Administrator");

		RoleCriteria roleCriteria = new RoleCriteria();
		roleCriteria.setAppName("projectx");
		roleCriteria.setAction(actionCriteria);
		roleCriteria.setLimit(10);
		roleCriteria.setOffset(0);
		roleCriteria.addSort(QRole.role.name, Sort.Direction.ASC);

		PaginatedResult<RoleDTO> page =
				roleService.findByPaging(roleCriteria, "Role(roleActions(action))");

		assertFalse(page.getData().isEmpty());

		assertTrue(
				page.getData().stream()
						.anyMatch(r -> "SUPER-USER".equals(r.getName()))
		);
	}

	@Test
	public void findByPaging_rejectsToManySorting() {

		ActionCriteria actionCriteria = new ActionCriteria();
		actionCriteria.setPage("Administrator");

		RoleCriteria roleCriteria = new RoleCriteria();
		roleCriteria.setAppName("projectx");
		roleCriteria.setAction(actionCriteria);
		roleCriteria.setLimit(10);
		roleCriteria.setOffset(0);

		roleCriteria.addSort(
				QRole.role.roleActions.any().actionId,
				Sort.Direction.ASC
		);

		try {
			roleService.findByPaging(
					roleCriteria,
					"Role(roleActions(action))"
			);
			fail("Expected PersistenceException due to unsafe to-many sorting");
		}
		catch (PersistenceException e) {

			Throwable root = e.getCause();
			while (root != null && root.getCause() != null) {
				root = root.getCause();
			}

			assertTrue(
					root instanceof IllegalStateException,
					"Root cause should be IllegalStateException"
			);

			assertTrue(
					root.getMessage().contains("Unsafe ORDER BY"),
					"Unexpected message: " + root.getMessage()
			);
		}
	}

	@Test
	public void findRole_byActionWithRoleBackReference() throws Exception {
		ActionCriteria actionCriteria = new ActionCriteria();

		RoleCriteria nestedRole = new RoleCriteria();
		nestedRole.setName("SUPER-USER");
		actionCriteria.setRole(nestedRole);

		RoleCriteria roleCriteria = new RoleCriteria();
		roleCriteria.setAction(actionCriteria);

		List<RoleDTO> roles =
				roleService.findAll(roleCriteria, "Role(roleActions(action))");

		assertFalse(roles.isEmpty());
		assertTrue(
				roles.stream().anyMatch(r -> "SUPER-USER".equals(r.getName()))
		);
	}

	@Test
	public void findByPaging_withNestedCriteria_multiSort_andOffsetLimit() throws Exception {

		// ------------------------------------------------------------------
		// GIVEN: nested Action criteria (forces EXISTS + ID-first paging)
		// ------------------------------------------------------------------

		ActionCriteria actionCriteria = new ActionCriteria();
		actionCriteria.setPage("Administrator");

		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setAction(actionCriteria);

		// ------------------------------------------------------------------
		// GIVEN: multi-column global sorting
		// ORDER BY roleType ASC, id DESC
		// ------------------------------------------------------------------

		criteria.addSort(QRole.role.roleType, Sort.Direction.ASC);
		criteria.addSort(QRole.role.id, Sort.Direction.DESC);

		// ------------------------------------------------------------------
		// GIVEN: paging via offset + limit
		// ------------------------------------------------------------------

		criteria.setOffset(0);
		criteria.setLimit(2);

		// ------------------------------------------------------------------
		// WHEN
		// ------------------------------------------------------------------

		PaginatedResult<RoleDTO> page =
				roleService.findByPaging(criteria, "Role(roleActions(action))");

		// ------------------------------------------------------------------
		// THEN: paging metadata
		// ------------------------------------------------------------------

		assertNotNull(page);
		assertTrue(page.getRecordsTotal() > 0);
		assertEquals(page.getSize(), 2);
		assertTrue(page.getNumberOfElements() <= 2);
		assertFalse(page.getData().isEmpty());

		List<RoleDTO> roles = page.getData();

		// ------------------------------------------------------------------
		// THEN: nested collections must be fetched
		// ------------------------------------------------------------------

		RoleDTO first = roles.getFirst();
		assertNotNull(first.getActions());
		assertFalse(first.getActions().isEmpty());

		// ------------------------------------------------------------------
		// THEN: ordering must respect DB semantics
		// roleType ASC (DB value), id DESC (within same roleType)
		// ------------------------------------------------------------------

		for (int i = 1; i < roles.size(); i++) {

			RoleDTO prev = roles.get(i - 1);
			RoleDTO curr = roles.get(i);

			// Only assert ID order when roleType is equal
			if (prev.getRoleType().equals(curr.getRoleType())) {
				assertTrue(
						prev.getId() >= curr.getId(),
						"Expected id DESC ordering within same roleType"
				);
			}
		}
	}
}
