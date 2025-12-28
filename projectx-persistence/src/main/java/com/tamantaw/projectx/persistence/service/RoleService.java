package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext;
import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.entity.*;
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
import org.jspecify.annotations.Nullable;
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
		Long,
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

	public RoleDTO create(
			RoleDTO dto,
			@Nullable Set<Long> actionIds,
			@Nullable Set<Long> administratorIds,
			long createdBy
	) throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "DTO must not be null");

		// Normalize optional relations
		Set<Long> safeActionIds =
				(actionIds != null ? actionIds : Set.of());

		Set<Long> safeAdministratorIds =
				(administratorIds != null ? administratorIds : Set.of());

		Assert.noNullElements(safeActionIds, "actionIds must not contain null elements");
		Assert.noNullElements(safeAdministratorIds, "administratorIds must not contain null elements");

		String c = String.format(
				"[service=%s][dto=%s]",
				serviceName(),
				dto.getClass().getSimpleName()
		);

		serviceLogger.info(
				"{} CREATE_WITH_ACTIONS_AND_ADMINS start createdBy={} actionCount={} adminCount={}",
				c,
				createdBy,
				safeActionIds.size(),
				safeAdministratorIds.size()
		);

		try {
			// ------------------------------------------------------------
			// 1. Create Role entity
			// ------------------------------------------------------------
			Role entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			// ------------------------------------------------------------
			// 2. Bind Role → Actions (optional)
			// ------------------------------------------------------------
			if (!safeActionIds.isEmpty()) {
				Set<RoleAction> roleActions =
						new HashSet<>(safeActionIds.size());

				for (Long actionId : safeActionIds) {
					RoleAction roleAction = new RoleAction();
					roleAction.setRole(entity);
					roleAction.setAction(
							entityManager.getReference(Action.class, actionId)
					);
					roleAction.setCreatedBy(createdBy);
					roleAction.setUpdatedBy(createdBy);
					roleActions.add(roleAction);
				}

				entity.setRoleActions(roleActions);
			}

			// ------------------------------------------------------------
			// 3. Bind Role → Administrators (optional)
			// ------------------------------------------------------------
			if (!safeAdministratorIds.isEmpty()) {
				Set<AdministratorRole> administratorRoles =
						new HashSet<>(safeAdministratorIds.size());

				for (Long administratorId : safeAdministratorIds) {
					AdministratorRole administratorRole = new AdministratorRole();
					administratorRole.setRole(entity);
					administratorRole.setAdministrator(
							entityManager.getReference(Administrator.class, administratorId)
					);
					administratorRole.setCreatedBy(createdBy);
					administratorRole.setUpdatedBy(createdBy);
					administratorRoles.add(administratorRole);
				}

				entity.setAdministratorRoles(administratorRoles);
			}

			// ------------------------------------------------------------
			// 4. Persist
			// ------------------------------------------------------------
			Role saved = roleRepository.saveRecord(entity);

			serviceLogger.info(
					"{} CREATE_WITH_ACTIONS_AND_ADMINS success id={} actions={} admins={}",
					c,
					saved.getId(),
					safeActionIds.size(),
					safeAdministratorIds.size()
			);

			return mapper.toDto(saved, mappingContext);
		}
		catch (DataIntegrityViolationException e) {
			serviceLogger.error(
					"{} CREATE_WITH_ACTIONS_AND_ADMINS integrity violation dto={} actions={} admins={}",
					c,
					dto,
					safeActionIds,
					safeAdministratorIds,
					e
			);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			serviceLogger.error(
					"{} CREATE_WITH_ACTIONS_AND_ADMINS failed dto={} actions={} admins={}",
					c,
					dto,
					safeActionIds,
					safeAdministratorIds,
					e
			);
			throw new PersistenceException(
					"CreateWithActionsAndAdmins failed dto=" + dto.getClass().getSimpleName(),
					e
			);
		}
	}

	public void updateRoleAndRelations(
			RoleDTO roleDTO,
			Set<Long> actionIds,
			Set<Long> administratorIds,
			long updatedBy
	) throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(roleDTO, "roleDTO must not be null");
		Assert.notNull(roleDTO.getId(), "Role ID must not be null");
		Assert.notNull(actionIds, "actionIds must not be null");
		Assert.noNullElements(actionIds, "actionIds must not contain null elements");
		Assert.notNull(administratorIds, "administratorIds must not be null");
		Assert.noNullElements(administratorIds, "administratorIds must not contain null elements");

		Long roleId = roleDTO.getId();

		String c = String.format(
				"[service=%s][domain=Role][id=%d]",
				serviceName(),
				roleId
		);

		serviceLogger.info(
				"{} UPDATE_WITH_ACTIONS_AND_ADMINS start actionCount={} adminCount={}",
				c,
				actionIds.size(),
				administratorIds.size()
		);

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
			Set<RoleAction> roleActions = role.getRoleActions();
			roleActions.size(); // force init

			Set<AdministratorRole> administratorRoles = role.getAdministratorRoles();
			administratorRoles.size(); // force init

			// ------------------------------------------------------------
			// 4. Remove obsolete Role → Action relations
			// ------------------------------------------------------------
			roleActions.removeIf(
					ra -> !actionIds.contains(ra.getAction().getId())
			);

			// ------------------------------------------------------------
			// 5. Add missing Role → Action relations
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
			// 6. Remove obsolete Role → Administrator relations
			// ------------------------------------------------------------
			administratorRoles.removeIf(
					ar -> !administratorIds.contains(ar.getAdministrator().getId())
			);

			// ------------------------------------------------------------
			// 7. Add missing Role → Administrator relations
			// ------------------------------------------------------------
			Set<Long> existingAdminIds = administratorRoles.stream()
					.map(ar -> ar.getAdministrator().getId())
					.collect(Collectors.toSet());

			for (Long administratorId : administratorIds) {
				if (existingAdminIds.contains(administratorId)) {
					continue;
				}

				AdministratorRole ar = new AdministratorRole();
				ar.setRole(role);
				ar.setAdministrator(
						entityManager.getReference(Administrator.class, administratorId)
				);
				ar.setCreatedBy(updatedBy);
				ar.setUpdatedBy(updatedBy);

				administratorRoles.add(ar);
			}

			// ------------------------------------------------------------
			// 8. Flush changes (no reassign collections!)
			// ------------------------------------------------------------
			entityManager.flush();

			serviceLogger.info(
					"{} UPDATE_WITH_ACTIONS_AND_ADMINS success roleId={} actions={} admins={} updatedBy={}",
					c,
					roleId,
					roleActions.size(),
					administratorRoles.size(),
					updatedBy
			);
		}
		catch (DataIntegrityViolationException e) {
			serviceLogger.error(
					"{} UPDATE_WITH_ACTIONS_AND_ADMINS integrity violation roleId={} actionIds={} adminIds={}",
					c,
					roleId,
					actionIds,
					administratorIds,
					e
			);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			serviceLogger.error(
					"{} UPDATE_WITH_ACTIONS_AND_ADMINS failed roleId={} actionIds={} adminIds={}",
					c,
					roleId,
					actionIds,
					administratorIds,
					e
			);
			throw new PersistenceException(
					"UpdateRoleAndActionsAndAdmins failed roleId=" + roleId,
					e
			);
		}
	}
}
