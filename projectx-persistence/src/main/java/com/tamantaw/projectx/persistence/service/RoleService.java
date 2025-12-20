package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.QRole;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.entity.RoleAction;
import com.tamantaw.projectx.persistence.exception.ContentNotFoundException;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.mapper.RoleMapper;
import com.tamantaw.projectx.persistence.repository.RoleRepository;
import com.tamantaw.projectx.persistence.service.base.BaseService;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tamantaw.projectx.persistence.utils.LoggerConstants.DATA_INTEGRITY_VIOLATION_MSG;

@Service
public class RoleService
		extends BaseService<
		Role,
		QRole,
		RoleCriteria,
		RoleDTO,
		RoleMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + RoleService.class.getSimpleName());

	private final EntityManager entityManager;

	@Autowired
	public RoleService(RoleRepository roleRepository, RoleMapper mapper, EntityManager entityManager) {

		super(roleRepository, mapper);
		this.entityManager = entityManager;
	}

	public Role create(RoleDTO dto, List<Long> actionIds, long createdBy)
			throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "DTO must not be null");
		Assert.notNull(actionIds, "actionIds must not be null");
		Assert.noNullElements(actionIds, "actionIds must not contain null elements");

		String c = String.format(
				"[service=%s][dto=%s]",
				serviceName(),
				dto.getClass().getSimpleName()
		);

		log.info("{} CREATE_WITH_ACTIONS start createdBy={} actionCount={}",
				c, createdBy, actionIds.size());

		try {
			Role entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			Set<Long> uniqueActionIds = new LinkedHashSet<>(actionIds);
			List<RoleAction> roleActions = new ArrayList<>(uniqueActionIds.size());

			for (Long actionId : uniqueActionIds) {
				RoleAction roleAction = new RoleAction();
				roleAction.setRole(entity);
				roleAction.setAction(entityManager.getReference(Action.class, actionId));
				roleAction.setCreatedBy(createdBy);
				roleAction.setUpdatedBy(createdBy);
				roleActions.add(roleAction);
			}

			entity.setRoleActions(roleActions);

			Role saved = repository.saveRecord(entity);

			log.info("{} CREATE_WITH_ACTIONS success id={} actions={}",
					c, saved.getId(), roleActions.size());
			return saved;
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} CREATE_WITH_ACTIONS integrity violation dto={} actions={}",
					c, dto, actionIds, e);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			log.error("{} CREATE_WITH_ACTIONS failed dto={} actions={}", c, dto, actionIds, e);
			throw new PersistenceException(
					"CreateWithActions failed dto=" + dto.getClass().getSimpleName(),
					e
			);
		}
	}

	public Role updateRoleAndActions(Long roleId, List<Long> actionIds)
			throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(roleId, "roleId must not be null");
		Assert.notNull(actionIds, "actionIds must not be null");
		Assert.noNullElements(actionIds, "actionIds must not contain null elements");

		String c = String.format(
				"[service=%s][domain=%s][id=%d]",
				serviceName(),
				"Role",
				roleId
		);

		log.info("{} UPDATE_WITH_ACTIONS start actionCount={}", c, actionIds.size());

		try {
			Role role = repository.findById(roleId)
					.orElseThrow(() -> new ContentNotFoundException("Role not found id=" + roleId));

			Set<Long> uniqueActionIds = new LinkedHashSet<>(actionIds);
			List<RoleAction> existingRoleActions = role.getRoleActions();

			// remove associations that are no longer requested
			existingRoleActions.removeIf(ra -> !uniqueActionIds.contains(ra.getAction().getId()));

			// add any missing associations
			Set<Long> existingActionIds = existingRoleActions.stream()
					.map(ra -> ra.getAction().getId())
					.collect(Collectors.toSet());

			for (Long actionId : uniqueActionIds) {
				if (existingActionIds.contains(actionId)) {
					continue;
				}

				RoleAction roleAction = new RoleAction();
				roleAction.setRole(role);
				roleAction.setAction(entityManager.getReference(Action.class, actionId));
				roleAction.setCreatedBy(role.getUpdatedBy());
				roleAction.setUpdatedBy(role.getUpdatedBy());
				existingRoleActions.add(roleAction);
			}

			Role saved = repository.saveRecord(role);

			log.info("{} UPDATE_WITH_ACTIONS success id={} actions={} updatedBy={} actionIds={}",
					c,
					saved.getId(),
					saved.getRoleActions().size(),
					saved.getUpdatedBy(),
					saved.getRoleActions()
						.stream()
						.map(RoleAction::getAction)
						.map(Action::getId)
						.collect(Collectors.toSet())
			);

			return saved;
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} UPDATE_WITH_ACTIONS integrity violation roleId={} actionIds={}",
					c, roleId, actionIds, e);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			log.error("{} UPDATE_WITH_ACTIONS failed roleId={} actionIds={}", c, roleId, actionIds, e);
			throw new PersistenceException("UpdateWithActions failed roleId=" + roleId, e);
		}
	}
}
