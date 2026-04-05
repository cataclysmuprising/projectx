package com.github.projectx.integrationTests;

import com.github.projectx.CommonTestBase;
import com.github.projectx.persistence.criteria.rba.RoleActionCriteria;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.rba.ActionDTO;
import com.github.projectx.persistence.dto.rba.RoleActionDTO;
import com.github.projectx.persistence.dto.rba.RoleDTO;
import com.github.projectx.persistence.entity.rba.Action;
import com.github.projectx.persistence.entity.rba.QRoleAction;
import com.github.projectx.persistence.entity.rba.Role;
import com.github.projectx.persistence.exception.BusinessException;
import com.github.projectx.persistence.exception.ConsistencyViolationException;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.service.rba.ActionService;
import com.github.projectx.persistence.service.rba.RoleActionService;
import com.github.projectx.persistence.service.rba.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class RoleActionServiceIT extends CommonTestBase {

	private static final String FETCH_GRAPH = "RoleAction(role,action)";

	@Autowired
	private RoleActionService roleActionService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private ActionService actionService;

	// ----------------------------------------------------------------------
	// FIND
	// ----------------------------------------------------------------------

	@Test(groups = "fetch")
	public void findById_existingRoleAction() throws Exception {

		Optional<RoleActionDTO> result =
				roleActionService.findById(1L);

		assertTrue(result.isPresent());

		RoleActionDTO dto = result.get();

		assertEquals(dto.getRoleId(), 1L);
		assertEquals(dto.getActionId(), 10011L);
	}

	@Test(groups = "fetch")
	public void findAll_byRoleId() throws Exception {

		RoleActionCriteria criteria = new RoleActionCriteria();
		criteria.setRoleId(2L);

		List<RoleActionDTO> mappings =
				roleActionService.findAll(criteria);

		assertEquals(mappings.size(), 3);

		assertTrue(
				mappings.stream()
						.allMatch(m -> m.getRoleId().equals(2L))
		);
	}

	@Test(groups = "fetch")
	public void findByPaging_basicPagination() throws Exception {
		RoleActionCriteria criteria = new RoleActionCriteria();
		criteria.setRoleId(2L);
		criteria.setLimit(1);
		criteria.setOffset(0);

		PaginatedResult<RoleActionDTO> page =
				roleActionService.findByPaging(criteria);

		assertNotNull(page);
		assertTrue(page.getTotalElements() >= 1);
		assertEquals(page.getPageSize(), 1);
		assertEquals(page.getPageNumber(), 0);
		assertEquals(page.getData().size(), 1);
	}

	@Test(groups = "fetch")
	public void findAll_withFetchGraph_includesRoleAndAction() throws Exception {
		RoleActionCriteria criteria = new RoleActionCriteria();
		criteria.setRoleId(2L);

		List<RoleActionDTO> mappings =
				roleActionService.findAll(criteria, FETCH_GRAPH);

		assertFalse(mappings.isEmpty());

		RoleActionDTO dto = mappings.getFirst();
		assertNotNull(dto.getRole());
		assertNotNull(dto.getAction());
	}

	// ----------------------------------------------------------------------
	// CREATE
	// ----------------------------------------------------------------------

	@Test(groups = "fetch")
	public void create_persistsRoleAction()
			throws ConsistencyViolationException, PersistenceException, BusinessException {

		RoleDTO newRole =
				createRole("TEMP_ROLE_ACTION");

		RoleActionDTO dto = new RoleActionDTO();
		dto.setRoleId(newRole.getId());
		dto.setActionId(10021L);

		RoleActionDTO saved =
				roleActionService.create(dto, TEST_CREATE_USER_ID);

		assertNotNull(saved.getId());
		assertEquals(newRole.getId(), saved.getRoleId());
		assertEquals(saved.getActionId(), 10021L);
		assertEquals(saved.getCreatedBy(), TEST_CREATE_USER_ID);
	}

	// ----------------------------------------------------------------------
	// UPDATE
	// ----------------------------------------------------------------------

	@Test(groups = "fetch")
	public void update_withDto_changesActionLink() throws Exception {

		RoleDTO newRole =
				createRole("TEMP_ROLE");

		RoleActionDTO createDto = new RoleActionDTO();
		createDto.setRoleId(newRole.getId());
		createDto.setActionId(10021L);

		RoleActionDTO saved =
				roleActionService.create(createDto, TEST_CREATE_USER_ID);

		RoleActionDTO updateDto = new RoleActionDTO();
		updateDto.setId(saved.getId());
		updateDto.setActionId(10022L);

		RoleActionDTO updated =
				roleActionService.update(updateDto, QRoleAction.roleAction, TEST_UPDATE_USER_ID);

		assertEquals(updated.getActionId(), 10022L);
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	// ----------------------------------------------------------------------
	// DELETE
	// ----------------------------------------------------------------------

	@Test(groups = "fetch")
	public void delete_removesRoleAction()
			throws ConsistencyViolationException, PersistenceException, BusinessException {

		RoleDTO newRole =
				createRole("TEMP_ROLE");

		ActionDTO newAction =
				createAction(
						"deleteActionLink",
						"/web/sec/actions/delete-link"
				);

		RoleActionDTO dto = new RoleActionDTO();
		dto.setRoleId(newRole.getId());
		dto.setActionId(newAction.getId());

		RoleActionDTO saved =
				roleActionService.create(dto, TEST_CREATE_USER_ID);

		RoleActionCriteria criteria = new RoleActionCriteria();
		criteria.setRoleId(newRole.getId());
		criteria.setActionId(newAction.getId());

		long deleted =
				roleActionService.delete(criteria);

		assertEquals(deleted, 1L);

		assertTrue(
				roleActionService.findById(saved.getId()).isEmpty()
		);
	}

	@Test(groups = "fetch")
	public void deleteById_removesRoleActionByIdentifier() throws Exception {

		RoleDTO newRole =
				createRole("TEMP_ROLE");

		ActionDTO newAction =
				createAction(
						"deleteByIdActionLink",
						"/web/sec/actions/delete-by-id-link"
				);

		RoleActionDTO dto = new RoleActionDTO();
		dto.setRoleId(newRole.getId());
		dto.setActionId(newAction.getId());

		RoleActionDTO saved =
				roleActionService.create(dto, TEST_CREATE_USER_ID);

		boolean deleted =
				roleActionService.deleteById(saved.getId());

		assertTrue(deleted);
		assertTrue(roleActionService.findById(saved.getId()).isEmpty());
	}

	// ----------------------------------------------------------------------
	// HELPERS
	// ----------------------------------------------------------------------

	private RoleDTO createRole(String name)
			throws ConsistencyViolationException, PersistenceException, BusinessException {

		RoleDTO dto = new RoleDTO();
		dto.setAppName("projectx");
		dto.setName(name);
		dto.setRoleType(Role.RoleType.CUSTOM);
		dto.setDescription("Role for role-action integration test");

		return roleService.create(dto, TEST_CREATE_USER_ID);
	}

	private ActionDTO createAction(String actionName, String url)
			throws ConsistencyViolationException, PersistenceException, BusinessException {

		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("Integration");
		dto.setActionName(actionName);
		dto.setDisplayName("Integration Action " + actionName);
		dto.setActionType(Action.ActionType.SUB);
		dto.setAccessLevel(Action.AccessLevel.WRITE);
		dto.setUrl(url);
		dto.setDescription("Action for role-action integration test");

		return actionService.create(dto, TEST_CREATE_USER_ID);
	}
}


