package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.AdministratorRole;
import com.tamantaw.projectx.persistence.entity.QAdministrator;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.ContentNotFoundException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.mapper.AdministratorMapper;
import com.tamantaw.projectx.persistence.repository.AdministratorRepository;
import com.tamantaw.projectx.persistence.repository.base.UpdateSpec;
import com.tamantaw.projectx.persistence.service.base.BaseService;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tamantaw.projectx.persistence.utils.LoggerConstants.DATA_INTEGRITY_VIOLATION_MSG;

@Service
public class AdministratorService
		extends BaseService<
		Administrator,
		QAdministrator,
		AdministratorCriteria,
		AdministratorDTO,
		AdministratorMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + AdministratorService.class.getSimpleName());

	private final EntityManager entityManager;
	private final AdministratorRepository administratorRepository;

	@Autowired
	public AdministratorService(
			AdministratorRepository administratorRepository,
			AdministratorMapper mapper,
			EntityManager entityManager
	) {
		super(administratorRepository, mapper);
		this.entityManager = entityManager;
		this.administratorRepository = administratorRepository;
	}

	public Administrator create(
			AdministratorDTO dto,
			List<Long> roleIds,
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
			// --------------------------------------------------
			// 1. Create Administrator
			// --------------------------------------------------
			Administrator entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			// IMPORTANT: administratorRoles must already be initialized
			List<AdministratorRole> roles = entity.getAdministratorRoles();

			// --------------------------------------------------
			// 2. Deduplicate role IDs
			// --------------------------------------------------
			Set<Long> uniqueRoleIds = new LinkedHashSet<>(roleIds);

			// --------------------------------------------------
			// 3. ADD roles (do NOT replace collection)
			// --------------------------------------------------
			for (Long roleId : uniqueRoleIds) {
				AdministratorRole ar = new AdministratorRole();
				ar.setAdministrator(entity);
				ar.setRole(entityManager.getReference(Role.class, roleId));
				ar.setCreatedBy(createdBy);
				ar.setUpdatedBy(createdBy);

				roles.add(ar); // ✅ mutate existing collection
			}

			Administrator saved = administratorRepository.saveRecord(entity);

			log.info("{} CREATE_WITH_ROLES success id={} roles={}",
					c, saved.getId(), saved.getAdministratorRoles().size());

			return saved;
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

	public void updateAdministratorAndRoles(
			AdministratorDTO dto,
			List<Long> roleIds,
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
			// --------------------------------------------------
			// 1. Update scalar fields ONLY
			// --------------------------------------------------
			UpdateSpec<Administrator> spec = buildUpdateSpecFromDto(dto);

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

			List<AdministratorRole> existingRoles =
					administrator.getAdministratorRoles();

			// 🔥 Force init to avoid orphanRemoval issues
			existingRoles.size();

			Set<Long> uniqueRoleIds = new LinkedHashSet<>(roleIds);

			// --------------------------------------------------
			// 3. Remove obsolete roles (orphanRemoval = true)
			// --------------------------------------------------
			existingRoles.removeIf(
					ar -> !uniqueRoleIds.contains(ar.getRole().getId())
			);

			// --------------------------------------------------
			// 4. Add missing roles
			// --------------------------------------------------
			Set<Long> existingRoleIds =
					existingRoles.stream()
							.map(ar -> ar.getRole().getId())
							.collect(Collectors.toSet());

			for (Long roleId : uniqueRoleIds) {
				if (existingRoleIds.contains(roleId)) {
					continue;
				}

				AdministratorRole ar = new AdministratorRole();
				ar.setAdministrator(administrator);
				ar.setRole(entityManager.getReference(Role.class, roleId));
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
}
