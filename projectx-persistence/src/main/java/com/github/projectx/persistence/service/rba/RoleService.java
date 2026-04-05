package com.github.projectx.persistence.service.rba;

import com.github.projectx.persistence.config.PersistenceContextNames;
import com.github.projectx.persistence.criteria.rba.RoleCriteria;
import com.github.projectx.persistence.dto.rba.RoleDTO;
import com.github.projectx.persistence.entity.rba.*;
import com.github.projectx.persistence.exception.ConsistencyViolationException;
import com.github.projectx.persistence.exception.ContentNotFoundException;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.mapper.rba.RoleMapper;
import com.github.projectx.persistence.repository.base.UpdateSpec;
import com.github.projectx.persistence.repository.rba.ActionRepository;
import com.github.projectx.persistence.repository.rba.AdministratorRepository;
import com.github.projectx.persistence.repository.rba.RoleActionRepository;
import com.github.projectx.persistence.repository.rba.RoleRepository;
import com.github.projectx.persistence.service.base.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.projectx.persistence.utils.LoggerConstants.DATA_INTEGRITY_VIOLATION_MSG;

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

	private final RoleRepository roleRepository;
	private final RoleActionRepository roleActionRepository;
	private final ActionRepository actionRepository;
	private final AdministratorRepository administratorRepository;

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 *
	 * @param roleRepository          input required by this operation contract
	 * @param mapper                  input required by this operation contract
	 * @param actionRepository        input required by this operation contract
	 * @param administratorRepository input required by this operation contract
	 */
	public RoleService(
			RoleRepository roleRepository,
			RoleMapper mapper,
			RoleActionRepository roleActionRepository,
			ActionRepository actionRepository,
			AdministratorRepository administratorRepository) {

		super(roleRepository, mapper);
		this.roleRepository = roleRepository;
		this.roleActionRepository = roleActionRepository;
		this.actionRepository = actionRepository;
		this.administratorRepository = administratorRepository;
	}

	/**
	 * Resolves role-name lookups for action IDs in one read path so permission checks avoid
	 * repeated repository calls and remain deterministic under load.
	 */
	@Transactional(transactionManager = PersistenceContextNames.TX_MANAGER, readOnly = true)
	public Map<Long, Set<String>> selectRoleNamesByAppName(
			String appName
	) throws PersistenceException {

		Assert.hasText(appName, "App name must not be blank");

		try {
			List<RoleActionRepository.RoleNameBindingRow> roleNameBindings =
					roleActionRepository.findRoleNameBindingsByAppName(appName);

			if (roleNameBindings.isEmpty()) {
				return Collections.emptyMap();
			}

			return roleNameBindings.stream()
					.filter(binding -> binding.actionId() != null)
					.filter(binding -> binding.roleName() != null && !binding.roleName().isBlank())
					.collect(Collectors.groupingBy(
							RoleActionRepository.RoleNameBindingRow::actionId,
							Collectors.collectingAndThen(
									Collectors.mapping(
											RoleActionRepository.RoleNameBindingRow::roleName,
											Collectors.toCollection(TreeSet::new)
									),
									Set::copyOf
							)
					));
		}
		catch (Exception e) {
			throw new PersistenceException(
					"Failed to fetch role names by appName=" + appName,
					e
			);
		}
	}

	/**
	 * Creates a role aggregate and its optional relation rows in one transaction so the role
	 * cannot be partially linked to actions/administrators.
	 */
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
			assertActionsExist(safeActionIds);
			assertAdministratorsExist(safeAdministratorIds);

			// ------------------------------------------------------------
			// 1. Create Role entity
			// ------------------------------------------------------------
			Role entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			Role saved = roleRepository.saveRecord(entity);

			// ------------------------------------------------------------
			// 2. Bind Role → Actions (optional)
			// ------------------------------------------------------------
			if (!safeActionIds.isEmpty()) {
				Set<RoleAction> roleActions =
						new HashSet<>(safeActionIds.size());

				for (Long actionId : safeActionIds) {
					RoleAction roleAction = new RoleAction();
					roleAction.setRole(saved);
					roleAction.setRoleId(saved.getId());
					roleAction.setActionId(actionId);
					Action action = new Action();
					action.setId(actionId);
					roleAction.setAction(action);
					roleAction.setCreatedBy(createdBy);
					roleAction.setUpdatedBy(createdBy);
					roleActions.add(roleAction);
				}

				saved.getRoleActions().addAll(roleActions);
			}

			// ------------------------------------------------------------
			// 3. Bind Role → Administrators (optional)
			// ------------------------------------------------------------
			if (!safeAdministratorIds.isEmpty()) {
				Set<AdministratorRole> administratorRoles =
						new HashSet<>(safeAdministratorIds.size());

				for (Long administratorId : safeAdministratorIds) {
					AdministratorRole administratorRole = new AdministratorRole();
					administratorRole.setRole(saved);
					administratorRole.setRoleId(saved.getId());
					administratorRole.setAdministratorId(administratorId);
					Administrator administrator = new Administrator();
					administrator.setId(administratorId);
					administratorRole.setAdministrator(administrator);
					administratorRole.setCreatedBy(createdBy);
					administratorRole.setUpdatedBy(createdBy);
					administratorRoles.add(administratorRole);
				}

				saved.getAdministratorRoles().addAll(administratorRoles);
			}

			// ------------------------------------------------------------
			// 4. Persist
			// ------------------------------------------------------------
			if ((saved.getRoleActions() != null && !saved.getRoleActions().isEmpty())
					|| (saved.getAdministratorRoles() != null && !saved.getAdministratorRoles().isEmpty())) {
				saved = roleRepository.saveRecord(saved);
			}

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

	/**
	 * Updates role scalar fields and relation memberships atomically so relation state is never
	 * left between old and new membership sets.
	 */
	public void updateRoleAndRelations(
			RoleDTO roleDTO,
			@Nullable Set<Long> actionIds,
			@Nullable Set<Long> administratorIds,
			long updatedBy
	) throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(roleDTO, "roleDTO must not be null");
		Assert.notNull(roleDTO.getId(), "Role ID must not be null");

		Set<Long> safeActionIds =
				(actionIds != null ? actionIds : Set.of());

		Set<Long> safeAdministratorIds =
				(administratorIds != null ? administratorIds : Set.of());

		Assert.noNullElements(safeActionIds, "actionIds must not contain null elements");
		Assert.noNullElements(safeAdministratorIds, "administratorIds must not contain null elements");

		Long roleId = roleDTO.getId();

		String c = String.format(
				"[service=%s][domain=Role][id=%d]",
				serviceName(),
				roleId
		);

		serviceLogger.info(
				"{} UPDATE_WITH_ACTIONS_AND_ADMINS start actionCount={} adminCount={}",
				c,
				safeActionIds.size(),
				safeAdministratorIds.size()
		);

		try {
			assertActionsExist(safeActionIds);
			assertAdministratorsExist(safeAdministratorIds);

			// ------------------------------------------------------------
			// 1. Update scalar fields only (safe, no joins)
			// ------------------------------------------------------------
			UpdateSpec<Role> spec = buildUpdateSpecFromDto(roleDTO, QRole.role);

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

			Set<AdministratorRole> administratorRoles = role.getAdministratorRoles();

			// ------------------------------------------------------------
			// 4. Remove obsolete Role → Action relations
			// ------------------------------------------------------------
			roleActions.removeIf(
					ra -> !safeActionIds.contains(ra.getActionId())
			);

			// ------------------------------------------------------------
			// 5. Add missing Role → Action relations
			// ------------------------------------------------------------
			Set<Long> existingActionIds = roleActions.stream()
					.map(RoleAction::getActionId)
					.collect(Collectors.toSet());

			for (Long actionId : safeActionIds) {
				if (existingActionIds.contains(actionId)) {
					continue;
				}

				RoleAction ra = new RoleAction();
				ra.setRole(role);
				ra.setRoleId(role.getId());
				ra.setActionId(actionId);
				Action action = new Action();
				action.setId(actionId);
				ra.setAction(action);
				ra.setCreatedBy(updatedBy);
				ra.setUpdatedBy(updatedBy);

				roleActions.add(ra);
			}

			// ------------------------------------------------------------
			// 6. Remove obsolete Role → Administrator relations
			// ------------------------------------------------------------
			administratorRoles.removeIf(
					ar -> !safeAdministratorIds.contains(ar.getAdministratorId())
			);

			// ------------------------------------------------------------
			// 7. Add missing Role → Administrator relations
			// ------------------------------------------------------------
			Set<Long> existingAdminIds = administratorRoles.stream()
					.map(AdministratorRole::getAdministratorId)
					.collect(Collectors.toSet());

			for (Long administratorId : safeAdministratorIds) {
				if (existingAdminIds.contains(administratorId)) {
					continue;
				}

				AdministratorRole ar = new AdministratorRole();
				ar.setRole(role);
				ar.setRoleId(role.getId());
				ar.setAdministratorId(administratorId);
				Administrator administrator = new Administrator();
				administrator.setId(administratorId);
				ar.setAdministrator(administrator);
				ar.setCreatedBy(updatedBy);
				ar.setUpdatedBy(updatedBy);

				administratorRoles.add(ar);
			}

			// ------------------------------------------------------------
			// 8. Flush changes (no reassign collections!)
			// ------------------------------------------------------------
			roleRepository.saveRecord(role);

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
					safeActionIds,
					safeAdministratorIds,
					e
			);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			serviceLogger.error(
					"{} UPDATE_WITH_ACTIONS_AND_ADMINS failed roleId={} actionIds={} adminIds={}",
					c,
					roleId,
					safeActionIds,
					safeAdministratorIds,
					e
			);
			throw new PersistenceException(
					"UpdateRoleAndActionsAndAdmins failed roleId=" + roleId,
					e
			);
		}
	}

	private void assertActionsExist(Set<Long> actionIds) {
		if (actionIds.isEmpty()) {
			return;
		}

		Set<Long> existing = actionRepository.findExistingIds(actionIds);
		if (existing.size() == actionIds.size()) {
			return;
		}

		Set<Long> missing = new LinkedHashSet<>(actionIds);
		missing.removeAll(existing);
		throw new ContentNotFoundException("Action not found ids=" + missing);
	}

	private void assertAdministratorsExist(Set<Long> administratorIds) {
		if (administratorIds.isEmpty()) {
			return;
		}

		Set<Long> existing = administratorRepository.findExistingIds(administratorIds);
		if (existing.size() == administratorIds.size()) {
			return;
		}

		Set<Long> missing = new LinkedHashSet<>(administratorIds);
		missing.removeAll(existing);
		throw new ContentNotFoundException("Administrator not found ids=" + missing);
	}
}

