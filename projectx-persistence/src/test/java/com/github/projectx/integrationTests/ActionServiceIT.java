package com.github.projectx.integrationTests;

import com.github.projectx.CommonTestBase;
import com.github.projectx.persistence.criteria.rba.ActionCriteria;
import com.github.projectx.persistence.criteria.rba.RoleCriteria;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.rba.ActionDTO;
import com.github.projectx.persistence.entity.rba.Action;
import com.github.projectx.persistence.entity.rba.QAction;
import com.github.projectx.persistence.exception.BusinessException;
import com.github.projectx.persistence.exception.ConsistencyViolationException;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.repository.base.UpdateSpec;
import com.github.projectx.persistence.service.rba.ActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class ActionServiceIT extends CommonTestBase {

	private static final String FETCH_GRAPH = "Action(roleActions(role))";

	@Autowired
	private ActionService actionService;

	@Test(groups = "fetch")
	public void findById_existingAction() throws Exception {
		Optional<ActionDTO> result = actionService.findById(10011L);

		assertTrue(result.isPresent());
		assertEquals(result.get().getActionName(), "dashboard");
		assertEquals(result.get().getPage(), "Dashboard");
		assertEquals(result.get().getAppName(), "projectx");
		assertEquals(result.get().getActionType(), Action.ActionType.MAIN);
		assertEquals(result.get().getAccessLevel(), Action.AccessLevel.READ);
	}

	@Test(groups = "fetch")
	public void findOne_byUrl() throws Exception {
		ActionCriteria criteria = new ActionCriteria();
		criteria.setUrl("/web/sec/administrator");

		Optional<ActionDTO> result = actionService.findOne(criteria);

		assertTrue(result.isPresent());
		assertEquals(result.get().getActionName(), "administratorList");
		assertEquals(result.get().getPage(), "Administrator");
	}

	@Test(groups = "fetch")
	public void findAll_byRoleCriteria() throws Exception {
		RoleCriteria roleCriteria = new RoleCriteria();
		roleCriteria.setName("SUPER-USER");

		ActionCriteria criteria = new ActionCriteria();
		criteria.setAppName("projectx");
		criteria.setRole(roleCriteria);

		List<ActionDTO> actions = actionService.findAll(criteria);

		assertFalse(actions.isEmpty());
		assertTrue(
				actions.stream().anyMatch(a -> "dashboard".equals(a.getActionName()))
		);
	}

	@Test(groups = "fetch")
	public void findAll_byAccessLevel() throws Exception {
		ActionCriteria criteria = new ActionCriteria();
		criteria.setAppName("projectx");
		criteria.setAccessLevel(Action.AccessLevel.SENSITIVE);

		List<ActionDTO> actions = actionService.findAll(criteria);

		assertFalse(actions.isEmpty());
		assertTrue(actions.stream().allMatch(action -> action.getAccessLevel() == Action.AccessLevel.SENSITIVE));
		assertTrue(actions.stream().anyMatch(action -> "administratorAdd".equals(action.getActionName())));
	}

	@Test(groups = "fetch")
	public void findByPaging_basicPagination() throws Exception {
		ActionCriteria criteria = new ActionCriteria();
		criteria.setAppName("projectx");
		criteria.setLimit(1);
		criteria.setOffset(0);

		PaginatedResult<ActionDTO> page = actionService.findByPaging(criteria);

		assertNotNull(page);
		assertTrue(page.getTotalElements() >= 1);
		assertEquals(page.getPageSize(), 1);
		assertEquals(page.getPageNumber(), 0);
		assertEquals(page.getData().size(), 1);
	}

	@Test(groups = "fetch")
	public void findAll_withFetchGraph_includesRoles() throws Exception {
		ActionCriteria criteria = new ActionCriteria();
		criteria.setActionName("dashboard");

		List<ActionDTO> actions = actionService.findAll(criteria, FETCH_GRAPH);

		assertFalse(actions.isEmpty());

		ActionDTO dto = actions.getFirst();
		assertNotNull(dto.getRoles());
		assertFalse(dto.getRoles().isEmpty());
	}

	@Test(groups = {"fetch", "insert"})
	public void create_persistsAction() throws ConsistencyViolationException, PersistenceException, BusinessException {
		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("Reports");
		dto.setActionName("reportsOverview");
		dto.setDisplayName("Reports overview");
		dto.setActionType(Action.ActionType.SUB);
		dto.setAccessLevel(Action.AccessLevel.READ);
		dto.setUrl("/web/sec/report");
		dto.setDescription("Report overview action for integration test");

		ActionDTO saved = actionService.create(dto, TEST_CREATE_USER_ID);

		assertNotNull(saved.getId());
		assertEquals(saved.getCreatedBy(), TEST_CREATE_USER_ID);
		assertEquals(saved.getUpdatedBy(), TEST_CREATE_USER_ID);
	}

	@Test(groups = {"fetch", "update"})
	public void update_updatesDisplayName() throws Exception {
		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("TempPage");
		dto.setActionName("tempActionUpdate");
		dto.setDisplayName("Temp action");
		dto.setActionType(Action.ActionType.SUB);
		dto.setAccessLevel(Action.AccessLevel.WRITE);
		dto.setUrl("/web/sec/temp");
		dto.setDescription("Temporary action for update test");

		actionService.create(dto, TEST_CREATE_USER_ID);

		ActionCriteria criteria = new ActionCriteria();
		criteria.setActionName("tempActionUpdate");

		UpdateSpec<Action> spec = (update, root) ->
				update.set(QAction.action.displayName, "Updated Temp Action");

		long affected = actionService.update(spec, criteria, TEST_UPDATE_USER_ID);

		assertEquals(affected, 1L);

		ActionDTO updated = actionService.findOne(criteria).orElseThrow();

		assertEquals(updated.getDisplayName(), "Updated Temp Action");
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	@Test(groups = {"fetch", "update"})
	public void update_withDto_appliesChangesAndAudit() throws Exception {
		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("DtoPage");
		dto.setActionName("dtoUpdateAction");
		dto.setDisplayName("DTO update action");
		dto.setActionType(Action.ActionType.SUB);
		dto.setAccessLevel(Action.AccessLevel.WRITE);
		dto.setUrl("/web/sec/dto-update-action");
		dto.setDescription("Temporary action for DTO update test");

		ActionDTO saved = actionService.create(dto, TEST_CREATE_USER_ID);

		ActionDTO updateDto = new ActionDTO();
		updateDto.setId(saved.getId());
		updateDto.setDisplayName("DTO Updated Action");
		updateDto.setDescription("Updated via DTO update method");

		ActionDTO updated = actionService.update(updateDto, QAction.action, TEST_UPDATE_USER_ID);

		assertEquals(updated.getDisplayName(), "DTO Updated Action");
		assertEquals(updated.getDescription(), "Updated via DTO update method");
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	@Test(groups = {"fetch", "delete"})
	public void delete_removesAction() throws ConsistencyViolationException, PersistenceException, BusinessException {
		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("TempPage");
		dto.setActionName("tempDeleteAction");
		dto.setDisplayName("Temp delete action");
		dto.setActionType(Action.ActionType.SUB);
		dto.setAccessLevel(Action.AccessLevel.WRITE);
		dto.setUrl("/web/sec/delete-action");
		dto.setDescription("Temporary action for delete test");

		ActionDTO saved = actionService.create(dto, TEST_CREATE_USER_ID);

		ActionCriteria criteria = new ActionCriteria();
		criteria.setActionName("tempDeleteAction");

		long deleted = actionService.delete(criteria);

		assertEquals(deleted, 1L);

		assertTrue(actionService.findById(saved.getId()).isEmpty());
	}

	@Test(groups = {"fetch", "delete"})
	public void deleteById_removesActionByIdentifier() throws ConsistencyViolationException, PersistenceException, BusinessException {
		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("TempPage");
		dto.setActionName("tempDeleteByIdAction");
		dto.setDisplayName("Temp delete-by-id action");
		dto.setActionType(Action.ActionType.SUB);
		dto.setAccessLevel(Action.AccessLevel.WRITE);
		dto.setUrl("/web/sec/delete-by-id-action");
		dto.setDescription("Temporary action for deleteById test");

		ActionDTO saved = actionService.create(dto, TEST_CREATE_USER_ID);

		boolean deleted = actionService.deleteById(saved.getId());

		assertTrue(deleted);
		assertTrue(actionService.findById(saved.getId()).isEmpty());
	}
}


