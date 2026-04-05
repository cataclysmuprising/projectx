package com.github.projectx.persistence.service.rba;

import com.github.projectx.persistence.criteria.rba.AdministratorCriteria;
import com.github.projectx.persistence.dto.rba.AdministratorDTO;
import com.github.projectx.persistence.entity.rba.Administrator;
import com.github.projectx.persistence.entity.rba.AdministratorRole;
import com.github.projectx.persistence.entity.rba.QAdministrator;
import com.github.projectx.persistence.entity.rba.Role;
import com.github.projectx.persistence.exception.ConsistencyViolationException;
import com.github.projectx.persistence.exception.ContentNotFoundException;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.mapper.rba.AdministratorMapper;
import com.github.projectx.persistence.repository.base.UpdateSpec;
import com.github.projectx.persistence.repository.rba.AdministratorRepository;
import com.github.projectx.persistence.repository.rba.AdministratorRoleRepository;
import com.github.projectx.persistence.repository.rba.RoleRepository;
import com.github.projectx.persistence.service.base.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.projectx.persistence.utils.LoggerConstants.DATA_INTEGRITY_VIOLATION_MSG;

@Service
public class AdministratorService
		extends BaseService<
		Long,
		Administrator,
		QAdministrator,
		AdministratorCriteria,
		AdministratorDTO,
		AdministratorMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + AdministratorService.class.getSimpleName());

	private final AdministratorRepository administratorRepository;
	private final RoleRepository roleRepository;
	private final AdministratorRoleRepository administratorRoleRepository;

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 *
	 * @param administratorRepository     input required by this operation contract
	 * @param mapper                      input required by this operation contract
	 * @param roleRepository              input required by this operation contract
	 * @param administratorRoleRepository input required by this operation contract
	 */
	public AdministratorService(
			AdministratorRepository administratorRepository,
			AdministratorMapper mapper,
			RoleRepository roleRepository,
			AdministratorRoleRepository administratorRoleRepository
	) {
		super(administratorRepository, mapper);
		this.administratorRepository = administratorRepository;
		this.roleRepository = roleRepository;
		this.administratorRoleRepository = administratorRoleRepository;
	}

	/**
	 * Creates an administrator and role memberships together so the account cannot be created
	 * without its intended authorization scope.
	 */
	public AdministratorDTO create(
			AdministratorDTO dto,
			Set<Long> roleIds,
			long createdBy
	) throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "DTO must not be null");
		Assert.notNull(roleIds, "roleIds must not be null");
		Assert.noNullElements(roleIds, "roleIds must not contain null elements");

		String c = String.format(
				"[service=%s][dto=%s]",
				serviceName(),
				dto.getClass().getSimpleName()
		);

		log.info("{} CREATE_WITH_ROLES start createdBy={} roleCount={}",
				c, createdBy, roleIds.size());

		try {
			assertRolesExist(roleIds);

			// --------------------------------------------------
			// 1. Create Administrator
			// --------------------------------------------------
			Administrator entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			Administrator savedAdministrator = administratorRepository.saveRecord(entity);

			// --------------------------------------------------
			// 2. ADD roles (do NOT replace collection)
			// --------------------------------------------------
			if (!roleIds.isEmpty()) {
				Set<AdministratorRole> roles = new HashSet<>(roleIds.size());
				for (Long roleId : roleIds) {
					AdministratorRole ar = new AdministratorRole();
					ar.setAdministratorId(savedAdministrator.getId());
					ar.setRoleId(roleId);
					ar.setAdministrator(savedAdministrator);
					Role role = new Role();
					role.setId(roleId);
					ar.setRole(role);
					ar.setCreatedBy(createdBy);
					ar.setUpdatedBy(createdBy);

					roles.add(ar);
				}
				administratorRoleRepository.saveAllRecords(roles);
				savedAdministrator.getAdministratorRoles().addAll(roles);
			}

			log.info("{} CREATE_WITH_ROLES success id={} roles={}",
					c, savedAdministrator.getId(), roleIds.size());

			return mapper.toDto(savedAdministrator, mappingContext);
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} CREATE_WITH_ROLES integrity violation dto={} roles={}",
					c, dto, roleIds, e);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			log.error("{} CREATE_WITH_ROLES failed dto={} roles={}",
					c, dto, roleIds, e);
			throw new PersistenceException(
					"CreateWithRoles failed dto=" + dto.getClass().getSimpleName(), e
			);
		}
	}

	/**
	 * Updates administrator scalar data and role memberships in one unit so authorization changes
	 * remain consistent with profile updates.
	 */
	public void updateAdministratorAndRoles(
			AdministratorDTO dto,
			Set<Long> roleIds,
			long updatedBy
	) throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "AdministratorDTO must not be null");
		Assert.notNull(dto.getId(), "AdministratorDTO.id must not be null");
		Assert.notNull(roleIds, "roleIds must not be null");
		Assert.noNullElements(roleIds, "roleIds must not contain null elements");

		Long adminId = dto.getId();

		String c = String.format(
				"[service=%s][domain=%s][id=%d]",
				serviceName(),
				"Administrator",
				adminId
		);

		log.info("{} UPDATE_WITH_ROLES start roleCount={}", c, roleIds.size());

		try {
			assertRolesExist(roleIds);

			// --------------------------------------------------
			// 1. Update scalar fields ONLY
			// --------------------------------------------------
			UpdateSpec<Administrator> spec = buildUpdateSpecFromDto(dto, QAdministrator.administrator);

			long affected =
					administratorRepository.updateById(spec, adminId, updatedBy);

			if (affected == 0) {
				throw new ContentNotFoundException(
						"Administrator not found id=" + adminId
				);
			}

			// --------------------------------------------------
			// 2. Load managed entity + roles
			// --------------------------------------------------
			Administrator administrator =
					administratorRepository.findById(adminId)
							.orElseThrow(() ->
									new ContentNotFoundException(
											"Administrator not found id=" + adminId
									));

			Set<AdministratorRole> existingRoles =
					administrator.getAdministratorRoles();

			// --------------------------------------------------
			// 3. Remove obsolete roles (orphanRemoval = true)
			// --------------------------------------------------
			existingRoles.removeIf(
					ar -> !roleIds.contains(ar.getRoleId())
			);

			// --------------------------------------------------
			// 4. Add missing roles
			// --------------------------------------------------
			Set<Long> existingRoleIds =
					existingRoles.stream()
							.map(AdministratorRole::getRoleId)
							.collect(Collectors.toSet());

			for (Long roleId : roleIds) {
				if (existingRoleIds.contains(roleId)) {
					continue;
				}

				AdministratorRole ar = new AdministratorRole();
				ar.setAdministrator(administrator);
				ar.setAdministratorId(administrator.getId());
				ar.setRoleId(roleId);
				Role role = new Role();
				role.setId(roleId);
				ar.setRole(role);
				ar.setCreatedBy(updatedBy);
				ar.setUpdatedBy(updatedBy);

				existingRoles.add(ar);
			}

			// --------------------------------------------------
			// 5. Persist (no collection replacement!)
			// --------------------------------------------------
			Administrator saved =
					administratorRepository.saveRecord(administrator);

			log.info("{} UPDATE_WITH_ROLES success id={} roles={} updatedBy={}",
					c,
					saved.getId(),
					saved.getAdministratorRoles().size(),
					updatedBy
			);
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} UPDATE_WITH_ROLES integrity violation adminId={} roleIds={}",
					c, adminId, roleIds, e);
			throw new ConsistencyViolationException(
					DATA_INTEGRITY_VIOLATION_MSG, e
			);
		}
		catch (Exception e) {
			log.error("{} UPDATE_WITH_ROLES failed adminId={} roleIds={}",
					c, adminId, roleIds, e);
			throw new PersistenceException(
					"UpdateWithRoles failed adminId=" + adminId, e
			);
		}
	}

	private void assertRolesExist(Set<Long> roleIds) {
		if (roleIds.isEmpty()) {
			return;
		}

		Set<Long> existing = roleRepository.findExistingIds(roleIds);
		if (existing.size() == roleIds.size()) {
			return;
		}

		Set<Long> missing = new HashSet<>(roleIds);
		missing.removeAll(existing);
		throw new ContentNotFoundException("Role not found ids=" + missing);
	}
}

