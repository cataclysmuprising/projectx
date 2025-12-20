package com.tamantaw.projectx.integrationTests;

import com.tamantaw.projectx.CommonTestBase;
import com.tamantaw.projectx.persistence.criteria.RoleActionCriteria;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.dto.RoleActionDTO;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.entity.RoleAction;
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

	@Test
	public void findById_existingRoleAction() throws Exception {
		Optional<RoleActionDTO> result = roleActionService.findById(1L);

		assertTrue(result.isPresent());
		RoleAction entity = entityManager.find(RoleAction.class, result.get().getId());
		assertNotNull(entity);
		assertEquals(entity.getRole().getId(), 1L);
		assertEquals(entity.getAction().getId(), 10011L);
	}

	@Test
	public void findAll_byRoleId() throws Exception {
		RoleActionCriteria criteria = new RoleActionCriteria();
		criteria.setRoleId(2L);

		List<RoleActionDTO> mappings = roleActionService.findAll(criteria);

		assertEquals(mappings.size(), 3);
		assertTrue(
				mappings.stream()
						.allMatch(m -> {
							RoleAction entity = entityManager.find(RoleAction.class, m.getId());
							return entity != null && entity.getRole().getId().equals(2L);
						})
		);
	}

	@Test
	public void create_persistsRoleAction() throws ConsistencyViolationException, PersistenceException {
		Role newRole = createRole("TEMP_ROLE_ACTION");

		RoleDTO roleRef = new RoleDTO();
		roleRef.setId(newRole.getId());

		RoleActionDTO dto = new RoleActionDTO();
		dto.setRole(roleRef);

		RoleAction saved = roleActionService.create(
				populateAction(dto, 10021L),
				TEST_CREATE_USER_ID
		);

		assertNotNull(saved.getId());
		assertEquals(saved.getRole().getId(), newRole.getId());
		assertEquals(saved.getAction().getId(), 10021L);
	}

	@Test
	public void delete_removesRoleAction() throws ConsistencyViolationException, PersistenceException {
		Role newRole = createRole("TEMP_ROLE_ACTION_DELETE");
		Action newAction = createAction("deleteActionLink", "^/sec/actions/delete-link$");

		RoleDTO roleRef = new RoleDTO();
		roleRef.setId(newRole.getId());

		ActionDTO actionRef = new ActionDTO();
		actionRef.setId(newAction.getId());

		RoleActionDTO dto = new RoleActionDTO();
		dto.setRole(roleRef);
		dto.setAction(actionRef);

		RoleAction saved = roleActionService.create(dto, TEST_CREATE_USER_ID);

		RoleActionCriteria criteria = new RoleActionCriteria();
		criteria.setRoleId(newRole.getId());
		criteria.setActionId(newAction.getId());

		long deleted = roleActionService.delete(criteria);

		assertEquals(deleted, 1L);

		entityManager.flush();
		entityManager.clear();

		assertNull(entityManager.find(RoleAction.class, saved.getId()));
	}

	private Role createRole(String name) throws ConsistencyViolationException, PersistenceException {
		RoleDTO dto = new RoleDTO();
		dto.setAppName("projectx");
		dto.setName(name);
		dto.setRoleType(Role.RoleType.CUSTOM);
		dto.setDescription("Role for role-action integration test");

		return roleService.create(dto, TEST_CREATE_USER_ID);
	}

	private RoleActionDTO populateAction(RoleActionDTO dto, long actionId) {
		ActionDTO actionRef = new ActionDTO();
		actionRef.setId(actionId);
		dto.setAction(actionRef);
		return dto;
	}

	private Action createAction(String actionName, String url)
			throws ConsistencyViolationException, PersistenceException {

		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("Integration");
		dto.setActionName(actionName);
		dto.setDisplayName("Integration Action " + actionName);
		dto.setActionType(Action.ActionType.SUB);
		dto.setUrl(url);
		dto.setDescription("Action for role-action delete test");

		return actionService.create(dto, TEST_CREATE_USER_ID);
	}
}
