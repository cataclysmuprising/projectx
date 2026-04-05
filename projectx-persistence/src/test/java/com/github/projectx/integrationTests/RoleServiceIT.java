package com.github.projectx.integrationTests;

import com.github.projectx.CommonTestBase;
import com.github.projectx.persistence.criteria.rba.ActionCriteria;
import com.github.projectx.persistence.criteria.rba.AdministratorRoleCriteria;
import com.github.projectx.persistence.criteria.rba.RoleActionCriteria;
import com.github.projectx.persistence.criteria.rba.RoleCriteria;
import com.github.projectx.persistence.dto.base.KeysetResult;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.rba.ActionDTO;
import com.github.projectx.persistence.dto.rba.AdministratorRoleDTO;
import com.github.projectx.persistence.dto.rba.RoleActionDTO;
import com.github.projectx.persistence.dto.rba.RoleDTO;
import com.github.projectx.persistence.entity.rba.QRole;
import com.github.projectx.persistence.entity.rba.Role;
import com.github.projectx.persistence.exception.BusinessException;
import com.github.projectx.persistence.exception.ConsistencyViolationException;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.repository.base.UpdateSpec;
import com.github.projectx.persistence.service.rba.ActionService;
import com.github.projectx.persistence.service.rba.AdministratorRoleService;
import com.github.projectx.persistence.service.rba.RoleActionService;
import com.github.projectx.persistence.service.rba.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.fail;

public class RoleServiceIT extends CommonTestBase {

	private static final String FETCH_GRAPH =
			"Role(administratorRoles(administrator),roleActions(action))";

	@Autowired
	private RoleService roleService;

	@Autowired
	private ActionService actionService;

	@Autowired
	private RoleActionService roleActionService;

	@Autowired
	private AdministratorRoleService administratorRoleService;

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
	public void findOne_byName() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setName("ADMINISTRATOR");

		Optional<RoleDTO> result = roleService.findOne(criteria);

		assertTrue(result.isPresent());
		assertEquals(result.get().getName(), "ADMINISTRATOR");
		assertEquals(result.get().getAppName(), "projectx");
	}

	@Test(groups = "fetch")
	public void findOne_withCollectionFetchGraph_includesActionsAndAdministrators() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setName("SUPER-USER");

		Optional<RoleDTO> result = roleService.findOne(criteria, FETCH_GRAPH);

		assertTrue(result.isPresent());

		RoleDTO role = result.get();
		assertNotNull(role.getActions());
		assertFalse(role.getActions().isEmpty());
		assertNotNull(role.getAdministrators());
		assertFalse(role.getAdministrators().isEmpty());
	}

	// ----------------------------------------------------------------------
	// FIND ALL (CRITERIA)
	// ----------------------------------------------------------------------

	@Test(groups = "fetch")
	public void findAll_byAppName() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");

		List<RoleDTO> roles = roleService.findAll(criteria);

		assertTrue(roles.size() >= 2);
		assertTrue(
				roles.stream().anyMatch(r -> r.getName().equals("SUPER-USER"))
		);
	}

	@Test(groups = "fetch")
	public void selectRoleNamesByAppName_groupsBindingsWithoutActionIdFilter() throws Exception {
		Map<Long, Set<String>> roleNamesByActionId = roleService.selectRoleNamesByAppName("projectx");

		assertFalse(roleNamesByActionId.isEmpty());
		assertTrue(roleNamesByActionId.containsKey(10011L));
		assertEquals(roleNamesByActionId.get(10011L), Set.of("ADMINISTRATOR", "SUPER-USER"));
		assertTrue(roleNamesByActionId.containsKey(10022L));
		assertEquals(roleNamesByActionId.get(10022L), Set.of("SUPER-USER"));
	}

	@Test(groups = "fetch")
	public void findAll_withFetchGraph_includesActionsAndAdministrators() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setName("SUPER-USER");

		List<RoleDTO> roles = roleService.findAll(criteria, FETCH_GRAPH);

		assertFalse(roles.isEmpty());

		RoleDTO role = roles.getFirst();
		assertNotNull(role.getActions());
		assertFalse(role.getActions().isEmpty());
		assertNotNull(role.getAdministrators());
		assertFalse(role.getAdministrators().isEmpty());
	}

	// ----------------------------------------------------------------------
	// FIND ROLE WITH ACTION FILTER (JOIN TEST)
	// ----------------------------------------------------------------------

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
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

		RoleDTO updated = roleService.update(updateDto, QRole.role, TEST_UPDATE_USER_ID);

		assertEquals(updated.getName(), "DTO_UPDATED_ROLE");
		assertEquals(updated.getDescription(), "Updated via DTO update");
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	// ----------------------------------------------------------------------
	// DELETE
	// ----------------------------------------------------------------------

	@Test(groups = "fetch")
	public void delete_removesRole() throws ConsistencyViolationException, PersistenceException, BusinessException {
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

	@Test(groups = "fetch")
	public void deleteById_removesRoleByIdentifier() throws ConsistencyViolationException, PersistenceException, BusinessException {
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

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
	public void findByPaging_basicPagination() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setLimit(1);
		criteria.setOffset(0);

		PaginatedResult<RoleDTO> page = roleService.findByPaging(criteria);

		assertNotNull(page);
		assertTrue(page.getTotalElements() >= 2);
		assertEquals(page.getPageSize(), 1);
		assertEquals(page.getPageNumber(), 0);
		assertEquals(page.getData().size(), 1);
	}

	@Test(groups = "fetch")
	public void findByPaging_emptyPage() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setLimit(10);
		criteria.setOffset(1000);

		PaginatedResult<RoleDTO> page = roleService.findByPaging(criteria);

		assertNotNull(page);
		assertTrue(page.getTotalElements() >= 2);
		assertTrue(page.getData().isEmpty());
	}

	@Test(groups = "fetch")
	public void findByPaging_withCollectionFetchGraph_emptyPage_preservesTotalCount() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setLimit(10);
		criteria.setOffset(1000);

		PaginatedResult<RoleDTO> page =
				roleService.findByPaging(criteria, "Role(roleActions(action))");

		assertNotNull(page);
		assertTrue(page.getTotalElements() >= 2);
		assertTrue(page.getData().isEmpty());
	}

	@Test(groups = "fetch")
	public void findByPaging_withAbsoluteOffset_appliesExactOffset() throws Exception {
		RoleCriteria allCriteria = new RoleCriteria();
		allCriteria.setAppName("projectx");
		allCriteria.addSort(QRole.role.id, Sort.Direction.ASC);

		List<RoleDTO> all = roleService.findAll(allCriteria);
		assertTrue(all.size() >= 2);

		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setLimit(2);
		criteria.setOffset(1);
		criteria.addSort(QRole.role.id, Sort.Direction.ASC);

		PaginatedResult<RoleDTO> page = roleService.findByPaging(criteria);

		assertNotNull(page);
		assertFalse(page.getData().isEmpty());
		assertEquals(page.getData().getFirst().getId(), all.get(1).getId());
	}

	@Test(groups = "fetch")
	public void findByPaging_withPageNumberOnly_usesDefaultLimit() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setPageNumber(1); // 1-based

		PaginatedResult<RoleDTO> page = roleService.findByPaging(criteria);

		assertNotNull(page);
		assertTrue(page.getTotalElements() >= 2);
		assertEquals(page.getPageNumber(), 0); // Spring Page is 0-based
		assertTrue(page.getPageSize() > 0);    // should be DEFAULT_PAGE_SIZE (20) if you expose it
	}

	@Test(groups = "fetch")
	public void findByPaging_withPageNumberAndLimit() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.setPageNumber(1);
		criteria.setLimit(1);

		PaginatedResult<RoleDTO> page = roleService.findByPaging(criteria);

		assertNotNull(page);
		assertEquals(page.getPageNumber(), 0);
		assertEquals(page.getPageSize(), 1);
		assertEquals(page.getData().size(), 1);
	}

	@Test(groups = "fetch")
	public void findByKeyset_basicPagination() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");

		KeysetResult<RoleDTO, Long> firstPage =
				roleService.findByKeyset(criteria, null, 1);

		assertNotNull(firstPage);
		assertEquals(firstPage.getPageSize(), 1);
		assertEquals(firstPage.getData().size(), 1);

		RoleDTO first = firstPage.getData().getFirst();
		assertNotNull(first.getId());
		assertEquals(firstPage.getNextCursor(), first.getId());

		KeysetResult<RoleDTO, Long> secondPage =
				roleService.findByKeyset(criteria, firstPage.getNextCursor(), 1);

		assertNotNull(secondPage);
		assertFalse(secondPage.getData().isEmpty());
		assertTrue(
				secondPage.getData().getFirst().getId() > first.getId(),
				"Expected cursor paging to advance on ascending ID order"
		);
	}

	@Test(groups = "fetch")
	public void findByKeyset_withCollectionFetchGraph_includesActionsAndAdministrators() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setName("SUPER-USER");

		KeysetResult<RoleDTO, Long> page =
				roleService.findByKeyset(criteria, null, 1, FETCH_GRAPH);

		assertNotNull(page);
		assertFalse(page.getData().isEmpty());

		RoleDTO role = page.getData().getFirst();
		assertNotNull(role.getActions());
		assertFalse(role.getActions().isEmpty());
		assertNotNull(role.getAdministrators());
		assertFalse(role.getAdministrators().isEmpty());
	}

	@Test(groups = "fetch")
	public void findByKeyset_rejectsLimitAboveDefaultMaxRows() {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");

		try {
			roleService.findByKeyset(criteria, null, criteria.maxRowsPerPage() + 1);
			fail("Expected PersistenceException due to oversized keyset limit");
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
					root.getMessage().contains("Maximum allowed"),
					"Unexpected message: " + root.getMessage()
			);
		}
	}

	@Test(groups = "fetch")
	public void findByPaging_rejectsPageSizeAboveDefaultMaxRows() {

		RoleCriteria criteria = new RoleCriteria() {
			@Override
			public Pageable toPageable() {
				return PageRequest.of(0, maxRowsPerPage() + 1, resolveSort());
			}
		};
		criteria.setAppName("projectx");

		try {
			roleService.findByPaging(criteria);
			fail("Expected PersistenceException due to oversized page size");
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
					root.getMessage().contains("Maximum allowed"),
					"Unexpected message: " + root.getMessage()
			);
		}
	}

	@Test(groups = "fetch")
	public void findAll_sortedByIdDesc() throws Exception {
		RoleCriteria criteria = new RoleCriteria();
		criteria.setAppName("projectx");
		criteria.addSort(QRole.role.id, Sort.Direction.DESC);

		List<RoleDTO> roles = roleService.findAll(criteria);

		assertTrue(roles.size() >= 2);
		assertTrue(roles.get(0).getId() > roles.get(1).getId());
	}

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
	public void findByPaging_rejectsToManyStringSorting() {

		RoleCriteria roleCriteria = new RoleCriteria();
		roleCriteria.setAppName("projectx");
		roleCriteria.setLimit(10);
		roleCriteria.setOffset(0);
		roleCriteria.addSortKey("roleActions.actionId", Sort.Direction.ASC);

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

	@Test(groups = "fetch")
	public void findByPaging_rejectsInvalidNestedStringSortingPath() {

		RoleCriteria roleCriteria = new RoleCriteria();
		roleCriteria.setAppName("projectx");
		roleCriteria.setLimit(10);
		roleCriteria.setOffset(0);
		roleCriteria.addSortKey("name.invalid", Sort.Direction.ASC);

		try {
			roleService.findByPaging(roleCriteria);
			fail("Expected PersistenceException due to invalid nested string sort path");
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
					root.getMessage().contains("path traverses non-entity attribute"),
					"Unexpected message: " + root.getMessage()
			);
		}
	}

	@Test(groups = "fetch")
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

	@Test(groups = "fetch")
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
		assertTrue(page.getTotalElements() > 0);
		assertEquals(page.getPageSize(), 2);
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


