package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.AdministratorLoginHistoryCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorLoginHistoryDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.AdministratorLoginHistory;
import com.tamantaw.projectx.persistence.entity.QAdministratorLoginHistory;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.mapper.AdministratorLoginHistoryMapper;
import com.tamantaw.projectx.persistence.repository.AdministratorLoginHistoryRepository;
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
public class AdministratorLoginHistoryService
		extends BaseService<
		AdministratorLoginHistory,
		QAdministratorLoginHistory,
		AdministratorLoginHistoryCriteria,
		AdministratorLoginHistoryDTO,
		AdministratorLoginHistoryMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + AdministratorLoginHistoryService.class.getSimpleName());

	private final EntityManager entityManager;
	private final AdministratorLoginHistoryRepository administratorLoginHistoryRepository;

	@Autowired
	public AdministratorLoginHistoryService(
			AdministratorLoginHistoryRepository administratorLoginHistoryRepository,
			AdministratorLoginHistoryMapper mapper,
			EntityManager entityManager
	) {
		super(administratorLoginHistoryRepository, mapper);
		this.entityManager = entityManager;
		this.administratorLoginHistoryRepository = administratorLoginHistoryRepository;
	}

	@Override
	public AdministratorLoginHistoryDTO create(
			AdministratorLoginHistoryDTO dto,
			long createdBy
	) throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "DTO must not be null");
		Assert.notNull(dto.getAdministratorId(), "administratorId must not be null");

		String c = String.format(
				"[service=%s][dto=%s]",
				serviceName(),
				dto.getClass().getSimpleName()
		);

		log.info("{} CREATE start createdBy={}", c, createdBy);

		try {
			// ------------------------------------------------------------
			// DTO → ENTITY (no relations)
			// ------------------------------------------------------------
			AdministratorLoginHistory entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			// ------------------------------------------------------------
			// Attach relation via reference (NO SQL, BeanValidation-safe)
			// ------------------------------------------------------------
			entity.setAdministrator(
					entityManager.getReference(
							Administrator.class,
							dto.getAdministratorId()
					)
			);

			AdministratorLoginHistory saved =
					administratorLoginHistoryRepository.saveRecord(entity);

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
