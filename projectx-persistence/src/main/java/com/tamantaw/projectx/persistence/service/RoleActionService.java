package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.RoleActionCriteria;
import com.tamantaw.projectx.persistence.dto.RoleActionDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.QRoleAction;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.entity.RoleAction;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.mapper.RoleActionMapper;
import com.tamantaw.projectx.persistence.mapper.base.MappingContext;
import com.tamantaw.projectx.persistence.repository.RoleActionRepository;
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
public class RoleActionService
		extends BaseService<
		RoleAction,
		QRoleAction,
		RoleActionCriteria,
		RoleActionDTO,
		RoleActionMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + RoleActionService.class.getSimpleName());

	private final EntityManager entityManager;
	private final RoleActionRepository roleActionRepository;
	private final MappingContext mappingContext;

	@Autowired
	public RoleActionService(
			RoleActionRepository roleActionRepository,
			RoleActionMapper mapper,
			EntityManager entityManager,
			MappingContext mappingContext) {
		super(roleActionRepository, mapper);
		this.entityManager = entityManager;
		this.roleActionRepository = roleActionRepository;
		this.mappingContext = mappingContext;
	}

	@Override
	public RoleActionDTO create(RoleActionDTO dto, long createdBy)
			throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "DTO must not be null");

		String c = String.format(
				"[service=%s][dto=%s]",
				serviceName(),
				dto.getClass().getSimpleName()
		);

		log.info("{} CREATE start createdBy={}", c, createdBy);

		try {
			RoleAction entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			applyRelations(entity, dto);

			RoleAction saved = roleActionRepository.saveRecord(entity);

			log.info("{} CREATE success id={}", c, saved.getId());
			return mapper.toDto(saved, mappingContext);
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

	private void applyRelations(RoleAction entity, RoleActionDTO dto) {
		if (dto.getRole() != null && dto.getRole().getId() != null) {
			entity.setRole(
					entityManager.getReference(Role.class, dto.getRole().getId())
			);
		}

		if (dto.getAction() != null && dto.getAction().getId() != null) {
			entity.setAction(
					entityManager.getReference(Action.class, dto.getAction().getId())
			);
		}
	}
}
