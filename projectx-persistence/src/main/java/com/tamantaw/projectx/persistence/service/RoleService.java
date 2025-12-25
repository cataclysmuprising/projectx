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
import com.tamantaw.projectx.persistence.repository.base.UpdateSpec;
import com.tamantaw.projectx.persistence.service.base.BaseService;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
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

	@Transactional(transactionManager = PrimaryPersistenceContext.TX_MANAGER, readOnly = true)
	public Map<Long, Set<String>> selectRoleNamesByActionIds(
			Set<Long> actionIds,
			String appName
	) throws PersistenceException {

		Assert.notEmpty(actionIds, "Action IDs must not be empty");
		Assert.notNull(appName, "App name must not be null");

		try {
			RoleCriteria roleCriteria = new RoleCriteria();
			roleCriteria.setAppName(appName);

			ActionCriteria actionCriteria = new ActionCriteria();
			actionCriteria.setIncludeIds(actionIds);
			actionCriteria.setAppName(appName);

			roleCriteria.setAction(actionCriteria);

			List<Role> roles = roleRepository.findAll(roleCriteria, "Role(roleActions)");

			if (roles == null || roles.isEmpty()) {
				return Collections.emptyMap();
			}

			// GROUP BY actionId
			return roles.stream()
					.filter(r -> r.getRoleActions() != null)
					.flatMap(r ->
							r.getRoleActions().stream()
									.map(ra -> Map.entry(
											ra.getAction().getId(),
											r.getName()
									))
					)
					.filter(e -> e.getKey() != null && e.getValue() != null)
					.collect(Collectors.groupingBy(
							Map.Entry::getKey,
							Collectors.mapping(
									Map.Entry::getValue,
									Collectors.toSet()
							)
					));
		}
		catch (Exception e) {
			throw new PersistenceException(
					"Failed to fetch role names for actionIds=" + actionIds,
					e
			);
		}
	}

	public Role create(RoleDTO dto, Set<Long> actionIds, long createdBy)
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

			List<RoleAction> roleActions = new ArrayList<>(actionIds.size());

			for (Long actionId : actionIds) {
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

	public void updateRoleAndActions(
			RoleDTO roleDTO,
			Set<Long> actionIds,
			long updatedBy
	) throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(roleDTO, "roleDTO must not be null");
		Assert.notNull(roleDTO.getId(), "Role ID must not be null");
		Assert.notNull(actionIds, "actionIds must not be null");
		Assert.noNullElements(actionIds, "actionIds must not contain null elements");

		Long roleId = roleDTO.getId();

		String c = String.format(
				"[service=%s][domain=Role][id=%d]",
				serviceName(),
				roleId
		);

		serviceLogger.info("{} UPDATE_WITH_ACTIONS start actionCount={}", c, actionIds.size());

		try {
			// ------------------------------------------------------------
			// 1. Update scalar fields only (safe, no joins)
			// ------------------------------------------------------------
			UpdateSpec<Role> spec = buildUpdateSpecFromDto(roleDTO);

			long affected = roleRepository.updateById(spec, roleId, updatedBy);
			if (affected == 0) {
				throw new ContentNotFoundException("Role not found id=" + roleId);
			}

			// ------------------------------------------------------------
			// 2. Reload managed Role entity
			// ------------------------------------------------------------
			Role role = roleRepository.findById(roleId)
					.orElseThrow(() -> new ContentNotFoundException("Role not found id=" + roleId));

			// ------------------------------------------------------------
			// 3. Force collection initialization (CRITICAL)
			// ------------------------------------------------------------
			List<RoleAction> roleActions = role.getRoleActions();
			roleActions.size(); // force init

			// ------------------------------------------------------------
			// 4. Remove obsolete relations
			// ------------------------------------------------------------
			roleActions.removeIf(
					ra -> !actionIds.contains(ra.getAction().getId())
			);

			// ------------------------------------------------------------
			// 5. Add missing relations
			// ------------------------------------------------------------
			Set<Long> existingActionIds = roleActions.stream()
					.map(ra -> ra.getAction().getId())
					.collect(Collectors.toSet());

			for (Long actionId : actionIds) {
				if (existingActionIds.contains(actionId)) {
					continue;
				}

				RoleAction ra = new RoleAction();
				ra.setRole(role);
				ra.setAction(entityManager.getReference(Action.class, actionId));
				ra.setCreatedBy(updatedBy);
				ra.setUpdatedBy(updatedBy);

				roleActions.add(ra);
			}

			// ------------------------------------------------------------
			// 6. Flush changes (no reassign collection!)
			// ------------------------------------------------------------
			entityManager.flush();

			serviceLogger.info(
					"{} UPDATE_WITH_ACTIONS success roleId={} actions={} updatedBy={}",
					c,
					roleId,
					roleActions.size(),
					updatedBy
			);
		}
		catch (DataIntegrityViolationException e) {
			serviceLogger.error(
					"{} UPDATE_WITH_ACTIONS integrity violation roleId={} actionIds={}",
					c,
					roleId,
					actionIds,
					e
			);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			serviceLogger.error(
					"{} UPDATE_WITH_ACTIONS failed roleId={} actionIds={}",
					c,
					roleId,
					actionIds,
					e
			);
			throw new PersistenceException(
					"UpdateRoleAndActions failed roleId=" + roleId,
					e
			);
		}
	}
}
