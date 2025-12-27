package com.tamantaw.projectx.integrationTests;

import com.tamantaw.projectx.CommonTestBase;
import com.tamantaw.projectx.persistence.criteria.RoleActionCriteria;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.dto.RoleActionDTO;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.ActionService;
import com.tamantaw.projectx.persistence.service.RoleActionService;
import com.tamantaw.projectx.persistence.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class RoleActionServiceIT extends CommonTestBase {

	@Autowired
	private RoleActionService roleActionService;

	@Autowired
	private RoleService roleService;

	@Autowired
	private ActionService actionService;

	// ----------------------------------------------------------------------
	// FIND
	// ----------------------------------------------------------------------

	@Test
	public void findById_existingRoleAction() throws Exception {

		Optional<RoleActionDTO> result =
				roleActionService.findById(1L);

		assertTrue(result.isPresent());

		RoleActionDTO dto = result.get();

		assertEquals(dto.getRoleId(), 1L);
		assertEquals(dto.getActionId(), 10011L);
	}

	@Test
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

	// ----------------------------------------------------------------------
	// CREATE
	// ----------------------------------------------------------------------

	@Test
	public void create_persistsRoleAction()
			throws ConsistencyViolationException, PersistenceException {

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

	@Test
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
				roleActionService.update(updateDto, TEST_UPDATE_USER_ID);

		assertEquals(updated.getActionId(), 10022L);
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	// ----------------------------------------------------------------------
	// DELETE
	// ----------------------------------------------------------------------

	@Test
	public void delete_removesRoleAction()
			throws ConsistencyViolationException, PersistenceException {

		RoleDTO newRole =
				createRole("TEMP_ROLE");

		ActionDTO newAction =
				createAction(
						"deleteActionLink",
						"^/sec/actions/delete-link$"
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

	@Test
	public void deleteById_removesRoleActionByIdentifier() throws Exception {

		RoleDTO newRole =
				createRole("TEMP_ROLE");

		ActionDTO newAction =
				createAction(
						"deleteByIdActionLink",
						"^/sec/actions/delete-by-id-link$"
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
			throws ConsistencyViolationException, PersistenceException {

		RoleDTO dto = new RoleDTO();
		dto.setAppName("projectx");
		dto.setName(name);
		dto.setRoleType(Role.RoleType.CUSTOM);
		dto.setDescription("Role for role-action integration test");

		return roleService.create(dto, TEST_CREATE_USER_ID);
	}

	private ActionDTO createAction(String actionName, String url)
			throws ConsistencyViolationException, PersistenceException {

		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("Integration");
		dto.setActionName(actionName);
		dto.setDisplayName("Integration Action " + actionName);
		dto.setActionType(Action.ActionType.SUB);
		dto.setUrl(url);
		dto.setDescription("Action for role-action integration test");

		return actionService.create(dto, TEST_CREATE_USER_ID);
	}
}
