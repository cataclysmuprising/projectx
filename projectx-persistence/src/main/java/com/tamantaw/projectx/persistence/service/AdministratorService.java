package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.QAdministrator;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.entity.AdministratorRole;
import com.tamantaw.projectx.persistence.exception.ContentNotFoundException;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.mapper.AdministratorMapper;
import com.tamantaw.projectx.persistence.repository.AdministratorRepository;
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

	@Autowired
	public AdministratorService(
			AdministratorRepository administratorRepository,
			AdministratorMapper mapper,
			EntityManager entityManager
	) {
		super(administratorRepository, mapper);
		this.entityManager = entityManager;
	}

	public Administrator create(AdministratorDTO dto, List<Long> roleIds, long createdBy)
			throws PersistenceException, ConsistencyViolationException {

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
			Administrator entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			Set<Long> uniqueRoleIds = new LinkedHashSet<>(roleIds);
			List<AdministratorRole> administratorRoles = new ArrayList<>(uniqueRoleIds.size());

			for (Long roleId : uniqueRoleIds) {
				AdministratorRole administratorRole = new AdministratorRole();
				administratorRole.setAdministrator(entity);
				administratorRole.setRole(entityManager.getReference(Role.class, roleId));
				administratorRole.setCreatedBy(createdBy);
				administratorRole.setUpdatedBy(createdBy);
				administratorRoles.add(administratorRole);
			}

			entity.setAdministratorRoles(administratorRoles);

			Administrator saved = repository.saveRecord(entity);

			log.info("{} CREATE_WITH_ROLES success id={} roles={}",
					c, saved.getId(), administratorRoles.size());
			return saved;
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} CREATE_WITH_ROLES integrity violation dto={} roles={}",
					c, dto, roleIds, e);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			log.error("{} CREATE_WITH_ROLES failed dto={} roles={}", c, dto, roleIds, e);
			throw new PersistenceException(
					"CreateWithRoles failed dto=" + dto.getClass().getSimpleName(),
					e
			);
		}
	}

	public Administrator updateAdministratorAndRoles(Long adminId, List<Long> roleIds)
			throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(adminId, "adminId must not be null");
		Assert.notNull(roleIds, "roleIds must not be null");
		Assert.noNullElements(roleIds, "roleIds must not contain null elements");

		String c = String.format(
				"[service=%s][domain=%s][id=%d]",
				serviceName(),
				"Administrator",
				adminId
		);

		log.info("{} UPDATE_WITH_ROLES start roleCount={}", c, roleIds.size());

		try {
			Administrator administrator = repository.findById(adminId)
					.orElseThrow(() -> new ContentNotFoundException("Administrator not found id=" + adminId));

			Set<Long> uniqueRoleIds = new LinkedHashSet<>(roleIds);
			List<AdministratorRole> existingRoles = administrator.getAdministratorRoles();

			// remove associations that are no longer requested
			existingRoles.removeIf(ar -> !uniqueRoleIds.contains(ar.getRole().getId()));

			// add any missing associations
			Set<Long> existingRoleIds = existingRoles.stream()
					.map(ar -> ar.getRole().getId())
					.collect(Collectors.toSet());

			for (Long roleId : uniqueRoleIds) {
				if (existingRoleIds.contains(roleId)) {
					continue;
				}

				AdministratorRole administratorRole = new AdministratorRole();
				administratorRole.setAdministrator(administrator);
				administratorRole.setRole(entityManager.getReference(Role.class, roleId));
				administratorRole.setCreatedBy(administrator.getUpdatedBy());
				administratorRole.setUpdatedBy(administrator.getUpdatedBy());
				existingRoles.add(administratorRole);
			}

			Administrator saved = repository.saveRecord(administrator);

			log.info("{} UPDATE_WITH_ROLES success id={} roles={} updatedBy={} roleIds={}",
					c,
					saved.getId(),
					saved.getAdministratorRoles().size(),
					saved.getUpdatedBy(),
					saved.getAdministratorRoles()
						.stream()
						.map(ar -> ar.getRole().getId())
						.collect(Collectors.toSet())
			);

			return saved;
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} UPDATE_WITH_ROLES integrity violation adminId={} roleIds={}",
					c, adminId, roleIds, e);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			log.error("{} UPDATE_WITH_ROLES failed adminId={} roleIds={}", c, adminId, roleIds, e);
			throw new PersistenceException("UpdateWithRoles failed adminId=" + adminId, e);
		}
	}
}
