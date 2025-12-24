package com.tamantaw.projectx.integrationTests;

import com.tamantaw.projectx.CommonTestBase;
import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.QAction;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.repository.base.UpdateSpec;
import com.tamantaw.projectx.persistence.service.ActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.*;

public class ActionServiceIT extends CommonTestBase {

	@Autowired
	private ActionService actionService;

	@Test
	public void findById_existingAction() throws Exception {
		Optional<ActionDTO> result = actionService.findById(10011L);

		assertTrue(result.isPresent());
		assertEquals(result.get().getActionName(), "dashboard");
		assertEquals(result.get().getPage(), "Dashboard");
		assertEquals(result.get().getAppName(), "projectx");
		assertEquals(result.get().getActionType(), Action.ActionType.MAIN);
	}

	@Test
	public void findOne_byUrl() throws Exception {
		ActionCriteria criteria = new ActionCriteria();
		criteria.setUrl("/web/sec/administrator");

		Optional<ActionDTO> result = actionService.findOne(criteria);

		assertTrue(result.isPresent());
		assertEquals(result.get().getActionName(), "administratorList");
		assertEquals(result.get().getPage(), "Administrator");
	}

	@Test
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

	@Test
	public void create_persistsAction() throws ConsistencyViolationException, PersistenceException {
		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("Reports");
		dto.setActionName("reportsOverview");
		dto.setDisplayName("Reports overview");
		dto.setActionType(Action.ActionType.SUB);
		dto.setUrl("/web/sec/report");
		dto.setDescription("Report overview action for integration test");

		ActionDTO saved = actionService.create(dto, TEST_CREATE_USER_ID);

		assertNotNull(saved.getId());
		assertEquals(saved.getCreatedBy(), TEST_CREATE_USER_ID);
		assertEquals(saved.getUpdatedBy(), TEST_CREATE_USER_ID);
	}

	@Test
	public void update_updatesDisplayName() throws Exception {
		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("TempPage");
		dto.setActionName("tempActionUpdate");
		dto.setDisplayName("Temp action");
		dto.setActionType(Action.ActionType.SUB);
		dto.setUrl("/web/sec/temp");
		dto.setDescription("Temporary action for update test");

		actionService.create(dto, TEST_CREATE_USER_ID);

		ActionCriteria criteria = new ActionCriteria();
		criteria.setActionName("tempActionUpdate");

		UpdateSpec<Action> spec = (update, root) ->
				update.set(QAction.action.displayName, "Updated Temp Action");

		long affected = actionService.update(spec, criteria, TEST_UPDATE_USER_ID);

		assertEquals(affected, 1L);

		Action updated = entityManager
				.createQuery(
						"select a from Action a where a.actionName = :name",
						Action.class
				)
				.setParameter("name", "tempActionUpdate")
				.getSingleResult();

		assertEquals(updated.getDisplayName(), "Updated Temp Action");
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	@Test
	public void update_withDto_appliesChangesAndAudit() throws Exception {
		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("DtoPage");
		dto.setActionName("dtoUpdateAction");
		dto.setDisplayName("DTO update action");
		dto.setActionType(Action.ActionType.SUB);
		dto.setUrl("/web/sec/dto-update-action");
		dto.setDescription("Temporary action for DTO update test");

		ActionDTO saved = actionService.create(dto, TEST_CREATE_USER_ID);

		ActionDTO updateDto = new ActionDTO();
		updateDto.setId(saved.getId());
		updateDto.setDisplayName("DTO Updated Action");
		updateDto.setDescription("Updated via DTO update method");

		ActionDTO updated = actionService.update(updateDto, TEST_UPDATE_USER_ID);

		assertEquals(updated.getDisplayName(), "DTO Updated Action");
		assertEquals(updated.getDescription(), "Updated via DTO update method");
		assertEquals(updated.getUpdatedBy(), TEST_UPDATE_USER_ID);
	}

	@Test
	public void delete_removesAction() throws ConsistencyViolationException, PersistenceException {
		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("TempPage");
		dto.setActionName("tempDeleteAction");
		dto.setDisplayName("Temp delete action");
		dto.setActionType(Action.ActionType.SUB);
		dto.setUrl("/web/sec/delete-action");
		dto.setDescription("Temporary action for delete test");

		ActionDTO saved = actionService.create(dto, TEST_CREATE_USER_ID);

		ActionCriteria criteria = new ActionCriteria();
		criteria.setActionName("tempDeleteAction");

		long deleted = actionService.delete(criteria);

		assertEquals(deleted, 1L);

		entityManager.flush();
		entityManager.clear();

		assertNull(entityManager.find(Action.class, saved.getId()));
	}

	@Test
	public void deleteById_removesActionByIdentifier() throws ConsistencyViolationException, PersistenceException {
		ActionDTO dto = new ActionDTO();
		dto.setAppName("projectx");
		dto.setPage("TempPage");
		dto.setActionName("tempDeleteByIdAction");
		dto.setDisplayName("Temp delete-by-id action");
		dto.setActionType(Action.ActionType.SUB);
		dto.setUrl("/web/sec/delete-by-id-action");
		dto.setDescription("Temporary action for deleteById test");

		ActionDTO saved = actionService.create(dto, TEST_CREATE_USER_ID);

		boolean deleted = actionService.deleteById(saved.getId());

		assertTrue(deleted);
		assertNull(entityManager.find(Action.class, saved.getId()));
	}
}
