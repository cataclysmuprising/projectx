package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext;
import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.QRole;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.entity.RoleAction;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.ContentNotFoundException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.mapper.RoleMapper;
import com.tamantaw.projectx.persistence.repository.RoleRepository;
import com.tamantaw.projectx.persistence.service.base.BaseService;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

import static com.tamantaw.projectx.persistence.utils.LoggerConstants.*;

@Service
public class RoleService
		extends BaseService<
		Role,
		QRole,
		RoleCriteria,
		RoleDTO,
		RoleMapper> {

	private static final Logger serviceLogger =
			LogManager.getLogger("serviceLogs." + RoleService.class.getSimpleName());

	private final EntityManager entityManager;
	private final RoleRepository roleRepository;

	@Autowired
	public RoleService(RoleRepository roleRepository, RoleMapper mapper, EntityManager entityManager) {

		super(roleRepository, mapper);
		this.entityManager = entityManager;
		this.roleRepository = roleRepository;
	}

	@Cacheable(
			cacheNames = "rolesByAction",
			key = "#appName + '|' + #actionId"
	)
	@Transactional(transactionManager = PrimaryPersistenceContext.TX_MANAGER, readOnly = true)
	public Set<String> selectRolesByActionId(Long actionId, String appName) throws PersistenceException {

		Assert.notNull(actionId, "Action ID shouldn't be null.");
		Assert.notNull(appName, "App Name shouldn't be null.");

		serviceLogger.debug(LOG_PREFIX + "Fetching role names by actionId=<{}>, appName=<{}>" + LOG_SUFFIX, actionId, appName);

		try {
			RoleCriteria roleCriteria = new RoleCriteria();
			roleCriteria.setAppName(appName);

			ActionCriteria actionCriteria = new ActionCriteria();
			actionCriteria.setId(actionId);
			actionCriteria.setAppName(appName);

			roleCriteria.setAction(actionCriteria);

			List<Role> roles = roleRepository.findAll(roleCriteria);

			if (roles == null || roles.isEmpty()) {
				return Collections.emptySet();
			}

			return roles.stream()
					.map(Role::getName)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());
		}
		catch (Exception e) {
			throw new PersistenceException("Failed to fetch role names for actionId=" + actionId + ", appName=" + appName, e);
		}
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

		serviceLogger.info("{} CREATE_WITH_ACTIONS start createdBy={} actionCount={}",
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

			Role saved = roleRepository.saveRecord(entity);

			serviceLogger.info("{} CREATE_WITH_ACTIONS success id={} actions={}",
					c, saved.getId(), roleActions.size());
			return saved;
		}
		catch (DataIntegrityViolationException e) {
			serviceLogger.error("{} CREATE_WITH_ACTIONS integrity violation dto={} actions={}",
					c, dto, actionIds, e);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			serviceLogger.error("{} CREATE_WITH_ACTIONS failed dto={} actions={}", c, dto, actionIds, e);
			throw new PersistenceException(
					"CreateWithActions failed dto=" + dto.getClass().getSimpleName(),
					e
			);
		}
	}

	@CacheEvict(cacheNames = "rolesByAction", allEntries = true)
	public void updateRoleAndActions(Long roleId, List<Long> actionIds)
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

		serviceLogger.info("{} UPDATE_WITH_ACTIONS start actionCount={}", c, actionIds.size());

		try {
			Role role = roleRepository.findById(roleId)
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

			Role saved = roleRepository.saveRecord(role);

			serviceLogger.info("{} UPDATE_WITH_ACTIONS success id={} actions={} updatedBy={} actionIds={}",
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
		}
		catch (DataIntegrityViolationException e) {
			serviceLogger.error("{} UPDATE_WITH_ACTIONS integrity violation roleId={} actionIds={}",
					c, roleId, actionIds, e);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			serviceLogger.error("{} UPDATE_WITH_ACTIONS failed roleId={} actionIds={}", c, roleId, actionIds, e);
			throw new PersistenceException("UpdateWithActions failed roleId=" + roleId, e);
		}
	}
}
