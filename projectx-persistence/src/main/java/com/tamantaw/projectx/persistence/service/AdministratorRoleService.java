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
		AdministratorRole,
		QAdministratorRole,
		AdministratorRoleCriteria,
		AdministratorRoleDTO,
		AdministratorRoleMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + AdministratorRoleService.class.getSimpleName());

	private final EntityManager entityManager;

	@Autowired
	public AdministratorRoleService(
			AdministratorRoleRepository administratorRoleRepository,
			AdministratorRoleMapper mapper,
			EntityManager entityManager
	) {
		super(administratorRoleRepository, mapper);
		this.entityManager = entityManager;
	}

	@Override
	public AdministratorRole create(AdministratorRoleDTO dto, long createdBy)
			throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "DTO must not be null");

		String c = String.format(
				"[service=%s][dto=%s]",
				serviceName(),
				dto.getClass().getSimpleName()
		);

		log.info("{} CREATE start createdBy={}", c, createdBy);

		try {
			AdministratorRole entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			applyRelations(entity, dto);

			AdministratorRole saved = repository.saveRecord(entity);

			log.info("{} CREATE success id={}", c, saved.getId());
			return saved;
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} CREATE integrity violation dto={}", c, dto, e);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			log.error("{} CREATE failed dto={}", c, dto, e);
			throw new PersistenceException(
					"Create failed dto=" + dto.getClass().getSimpleName(), e
			);
		}
	}

	private void applyRelations(AdministratorRole entity, AdministratorRoleDTO dto) {
		if (dto.getAdministrator() != null && dto.getAdministrator().getId() != null) {
			entity.setAdministrator(
					entityManager.getReference(Administrator.class, dto.getAdministrator().getId())
			);
		}

		if (dto.getRole() != null && dto.getRole().getId() != null) {
			entity.setRole(
					entityManager.getReference(Role.class, dto.getRole().getId())
			);
		}
	}
}
