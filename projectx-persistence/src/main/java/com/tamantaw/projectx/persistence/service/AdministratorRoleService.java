package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.AdministratorRoleCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorRoleDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.AdministratorRole;
import com.tamantaw.projectx.persistence.entity.QAdministratorRole;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.mapper.AdministratorRoleMapper;
import com.tamantaw.projectx.persistence.repository.AdministratorRoleRepository;
import com.tamantaw.projectx.persistence.service.base.BaseService;
import jakarta.persistence.EntityManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import static com.tamantaw.projectx.persistence.utils.LoggerConstants.DATA_INTEGRITY_VIOLATION_MSG;

@Service
public class AdministratorRoleService
		extends BaseService<
		Long,
		AdministratorRole,
		QAdministratorRole,
		AdministratorRoleCriteria,
		AdministratorRoleDTO,
		AdministratorRoleMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + AdministratorRoleService.class.getSimpleName());

	private final EntityManager entityManager;
	private final AdministratorRoleRepository administratorRoleRepository;

	@Autowired
	public AdministratorRoleService(
			AdministratorRoleRepository administratorRoleRepository,
			AdministratorRoleMapper mapper,
			EntityManager entityManager
	) {
		super(administratorRoleRepository, mapper);
		this.entityManager = entityManager;
		this.administratorRoleRepository = administratorRoleRepository;
	}

	@Override
	public AdministratorRoleDTO create(
			AdministratorRoleDTO dto,
			long createdBy)
			throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "DTO must not be null");
		Assert.notNull(dto.getAdministratorId(), "administratorId must not be null");
		Assert.notNull(dto.getRoleId(), "roleId must not be null");

		String c = String.format(
				"[service=%s][dto=%s]",
				serviceName(),
				dto.getClass().getSimpleName()
		);

		log.info("{} CREATE start createdBy={}", c, createdBy);

		try {
			// ------------------------------------------------------------
			// Map scalar fields only
			// ------------------------------------------------------------
			AdministratorRole entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			// ------------------------------------------------------------
			// Attach relations via references (NO SQL)
			// ------------------------------------------------------------
			entity.setAdministrator(
					entityManager.getReference(
							Administrator.class,
							dto.getAdministratorId()
					)
			);

			entity.setRole(
					entityManager.getReference(
							Role.class,
							dto.getRoleId()
					)
			);

			// ------------------------------------------------------------
			// Persist
			// ------------------------------------------------------------
			AdministratorRole saved =
					administratorRoleRepository.saveRecord(entity);

			log.info("{} CREATE success id={}", c, saved.getId());

			return mapper.toDto(saved, mappingContext);
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} CREATE integrity violation dto={}", c, dto, e);
			throw new ConsistencyViolationException(
					DATA_INTEGRITY_VIOLATION_MSG, e
			);
		}
		catch (Exception e) {
			log.error("{} CREATE failed dto={}", c, dto, e);
			throw new PersistenceException(
					"Create failed dto=" + dto.getClass().getSimpleName(), e
			);
		}
	}
}
