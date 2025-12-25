package com.tamantaw.projectx.persistence.service.base;

import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.tamantaw.projectx.persistence.config.PrimaryPersistenceContext;
import com.tamantaw.projectx.persistence.criteria.base.AbstractCriteria;
import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import com.tamantaw.projectx.persistence.dto.base.PaginatedResult;
import com.tamantaw.projectx.persistence.entity.base.AbstractEntity;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.mapper.base.AbstractMapper;
import com.tamantaw.projectx.persistence.mapper.base.MappingContext;
import com.tamantaw.projectx.persistence.repository.base.AbstractRepository;
import com.tamantaw.projectx.persistence.repository.base.UpdateSpec;
import jakarta.persistence.EntityNotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.tamantaw.projectx.persistence.utils.LoggerConstants.DATA_INTEGRITY_VIOLATION_MSG;

@Transactional(transactionManager = PrimaryPersistenceContext.TX_MANAGER, rollbackFor = Exception.class)
public abstract class BaseService<
		ID extends Serializable,
		ENTITY extends AbstractEntity,
		QCLAZZ extends EntityPathBase<ENTITY>,
		CRITERIA extends AbstractCriteria<QCLAZZ>,
		DTO extends AbstractDTO,
		MAPPER extends AbstractMapper<DTO, ENTITY>
		> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + BaseService.class.getSimpleName());

	protected final MAPPER mapper;
	protected final AbstractRepository<ID, ENTITY, QCLAZZ, CRITERIA> repository;

	@Autowired
	protected MappingContext mappingContext;

	protected BaseService(
			AbstractRepository<ID, ENTITY, QCLAZZ, CRITERIA> repository,
			MAPPER mapper
	) {
		this.repository = repository;
		this.mapper = mapper;
	}

// ----------------------------------------------------------------------
// Context helpers (KEEP)
// ----------------------------------------------------------------------

	protected String serviceName() {
		return getClass().getSimpleName();
	}

	protected String domainName(CRITERIA criteria) {
		return (criteria != null && criteria.getObjectClass() != null)
				? criteria.getObjectClass().getSimpleName()
				: "UnknownDomain";
	}

	protected String criteriaName(CRITERIA criteria) {
		return (criteria != null)
				? criteria.getClass().getSimpleName()
				: "NoCriteria";
	}

	protected String ctx(CRITERIA criteria) {
		return String.format(
				"[service=%s][domain=%s][criteria=%s]",
				serviceName(),
				domainName(criteria),
				criteriaName(criteria)
		);
	}

// ----------------------------------------------------------------------
// READ (ENTITY → DTO)
// ----------------------------------------------------------------------

	@Transactional(readOnly = true)
	public Optional<DTO> findById(ID id) throws PersistenceException {

		String c = String.format(
				"[service=%s][domain=%s][id=%s]",
				serviceName(),
				"ById",
				id
		);

		log.info("{} FIND_BY_ID start", c);

		try {
			Optional<ENTITY> entity = repository.findById(id);
			Optional<DTO> dto = entity.map(e -> mapper.toDto(e, mappingContext));

			log.info("{} FIND_BY_ID result found={}", c, dto.isPresent());
			return dto;
		}
		catch (Exception e) {
			log.error("{} FIND_BY_ID failed", c, e);
			throw new PersistenceException("FindById failed id=" + id, e);
		}
	}

	@Transactional(readOnly = true)
	public Optional<DTO> findOne(CRITERIA criteria, String... hints) throws PersistenceException {

		Assert.notNull(criteria, "Criteria must not be null");

		String c = ctx(criteria);
		log.info("{} FIND_ONE start criteria={} , hints={}", c, criteria, hints);

		try {
			Optional<ENTITY> entity = repository.findOne(criteria, hints);
			Optional<DTO> dto = entity.map(e -> mapper.toDto(e, mappingContext));

			log.info("{} FIND_ONE result found={}", c, dto.isPresent());
			return dto;
		}
		catch (Exception e) {
			log.error("{} FIND_ONE failed criteria={}, hints={}", c, criteria, hints, e);
			throw new PersistenceException(
					"FindOne failed criteria=" + criteriaName(criteria), e
			);
		}
	}

	@Transactional(readOnly = true)
	public List<DTO> findAll(CRITERIA criteria, String... hints) throws PersistenceException {

		Assert.notNull(criteria, "Criteria must not be null");

		String c = ctx(criteria);
		log.info("{} FIND_ALL start criteria={}, hints={}", c, criteria, hints);

		try {
			List<ENTITY> entities = repository.findAll(criteria, hints);
			List<DTO> dtos = mapper.mapToDtoList(entities, mappingContext);

			log.info("{} FIND_ALL success size={}", c, dtos.size());
			return dtos;
		}
		catch (Exception e) {
			log.error("{} FIND_ALL failed criteria={}, hints={}", c, criteria, hints, e);
			throw new PersistenceException(
					"FindAll failed criteria=" + criteriaName(criteria), e
			);
		}
	}

	@Transactional(readOnly = true)
	public PaginatedResult<DTO> findByPaging(CRITERIA criteria, String... hints)
			throws PersistenceException {

		Pageable pageable = criteria.toPageable();
		Assert.notNull(criteria, "Criteria must not be null");
		Assert.notNull(pageable, "Pageable must not be null");

		String c = ctx(criteria);
		log.info("{} FIND_PAGE start pageable={} , criteria={}, hints={}", c, pageable, criteria, hints);

		try {
			Page<ENTITY> page = repository.findByPaging(criteria, hints);

			Page<DTO> dtoPage = page.map(e -> mapper.toDto(e, mappingContext));

			log.info("{} FIND_PAGE success total={} size={}",
					c, dtoPage.getTotalElements(), dtoPage.getNumberOfElements());

			List<DTO> data = dtoPage.getContent();

			return new PaginatedResult<>(
					dtoPage.getTotalElements(),
					dtoPage.getTotalElements(),
					dtoPage.getTotalPages(),
					dtoPage.getSize(),
					dtoPage.getNumber(),
					dtoPage.getNumberOfElements(),
					dtoPage.getSort(),
					data
			);
		}
		catch (Exception e) {
			log.error("{} FIND_PAGE failed pageable={} criteria={} , hints={}",
					c, pageable, criteria, hints, e);

			throw new PersistenceException(
					"FindPage failed criteria=" + criteriaName(criteria), e
			);
		}
	}

	@Transactional(readOnly = true)
	public boolean exists(CRITERIA criteria) throws PersistenceException {

		Assert.notNull(criteria, "Criteria must not be null");

		String c = ctx(criteria);
		log.info("{} EXISTS start criteria={}", c, criteria);

		try {
			boolean exists = repository.exists(criteria);
			log.info("{} EXISTS result={}", c, exists);
			return exists;
		}
		catch (Exception e) {
			log.error("{} EXISTS failed criteria={}", c, criteria, e);
			throw new PersistenceException(
					"Exists failed criteria=" + criteriaName(criteria), e
			);
		}
	}

	@Transactional(readOnly = true)
	public long count(CRITERIA criteria) throws PersistenceException {

		Assert.notNull(criteria, "Criteria must not be null");

		String c = ctx(criteria);
		log.info("{} COUNT start criteria={}", c, criteria);

		try {
			long count = repository.count(criteria);
			log.info("{} COUNT result={}", c, count);
			return count;
		}
		catch (Exception e) {
			log.error("{} COUNT failed criteria={}", c, criteria, e);
			throw new PersistenceException(
					"Count failed criteria=" + criteriaName(criteria), e
			);
		}
	}

// ----------------------------------------------------------------------
// CREATE
// ----------------------------------------------------------------------

	public DTO create(DTO dto, long createdBy) throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "DTO must not be null");

		String c = String.format(
				"[service=%s][dto=%s]",
				serviceName(),
				dto.getClass().getSimpleName()
		);

		log.info("{} CREATE start createdBy={}", c, createdBy);

		try {
			ENTITY entity = mapper.toEntity(dto);
			entity.setCreatedBy(createdBy);
			entity.setUpdatedBy(createdBy);

			ENTITY saved = repository.saveRecord(entity);

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

	public List<ENTITY> createAll(List<DTO> dtos, long createdBy)
			throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dtos, "DTO list must not be null");

		String c = String.format(
				"[service=%s][dto=%s]",
				serviceName(),
				dtos.isEmpty() ? "EmptyList" : dtos.getFirst().getClass().getSimpleName()
		);

		log.info("{} CREATE_ALL start createdBy={} size={}", c, createdBy, dtos.size());

		try {
			if (dtos.isEmpty()) {
				log.info("{} CREATE_ALL success size=0", c);
				return List.of();
			}

			List<ENTITY> entities = new ArrayList<>(dtos.size());
			for (DTO dto : dtos) {
				ENTITY entity = mapper.toEntity(dto);
				entity.setCreatedBy(createdBy);
				entity.setUpdatedBy(createdBy);
				entities.add(entity);
			}

			List<ENTITY> saved = repository.saveAllRecords(entities);

			log.info("{} CREATE_ALL success savedSize={}", c, saved.size());
			return saved;
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} CREATE_ALL integrity violation size={}", c, dtos.size(), e);
			throw new ConsistencyViolationException(DATA_INTEGRITY_VIOLATION_MSG, e);
		}
		catch (Exception e) {
			log.error("{} CREATE_ALL failed size={}", c, dtos.size(), e);
			throw new PersistenceException(
					"CreateAll failed dto=" + (dtos.isEmpty() ? "EmptyList" : dtos.getFirst().getClass().getSimpleName()),
					e
			);
		}
	}

// ----------------------------------------------------------------------
// UPDATE / DELETE
// ----------------------------------------------------------------------

	public DTO update(DTO dto, long updatedBy) throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "DTO must not be null");
		Assert.notNull(dto.getId(), "DTO id must not be null");

		@SuppressWarnings("unchecked")
		ID id = (ID) dto.getId();

		String c = String.format(
				"[service=%s][dto=%s][id=%s]",
				serviceName(),
				dto.getClass().getSimpleName(),
				id
		);

		log.info("{} UPDATE_BY_ID start updatedBy={}", c, updatedBy);

		try {
			UpdateSpec<ENTITY> spec = buildUpdateSpecFromDto(dto);

			long affected = repository.updateById(spec, id, updatedBy);

			if (affected == 0) {
				throw new EntityNotFoundException(
						"Entity not found for id=" + id
				);
			}

			return findById(id).orElseThrow(
					() -> new IllegalStateException(
							"Entity disappeared after update id=" + id
					)
			);
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} UPDATE_BY_ID integrity violation dto={}", c, dto, e);
			throw new ConsistencyViolationException(
					DATA_INTEGRITY_VIOLATION_MSG, e
			);
		}
		catch (Exception e) {
			log.error("{} UPDATE_BY_ID failed dto={}", c, dto, e);
			throw new PersistenceException(
					"UpdateById failed dto=" + dto.getClass().getSimpleName(), e
			);
		}
	}

	public long update(UpdateSpec<ENTITY> spec, CRITERIA criteria, long updatedBy)
			throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(spec, "UpdateSpec must not be null");
		Assert.notNull(criteria, "Criteria must not be null");

		String c = ctx(criteria);
		log.info("{} UPDATE start updatedBy={} , criteria={}",
				c, updatedBy, criteria);

		try {
			long affected = repository.updateByCriteria(spec, criteria, updatedBy);

			log.info("{} UPDATE success affectedRows={}", c, affected);
			return affected;
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} UPDATE integrity violation criteria={}", c, criteria, e);
			throw new ConsistencyViolationException(
					DATA_INTEGRITY_VIOLATION_MSG, e
			);
		}
		catch (Exception e) {
			log.error("{} UPDATE failed criteria={}", c, criteria, e);
			throw new PersistenceException(
					"Update failed criteria=" + criteriaName(criteria), e
			);
		}
	}

	public boolean deleteById(ID id) throws PersistenceException {

		String c = String.format(
				"[service=%s][domain=%s][id=%s]",
				serviceName(),
				"DeleteById",
				id
		);

		try {
			return repository.deleteWithId(id);
		}
		catch (Exception e) {
			log.error("{} FIND_BY_ID failed", c, e);
			throw new PersistenceException("DeleteById failed id=" + id, e);
		}
	}

	public long delete(CRITERIA criteria)
			throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(criteria, "Criteria must not be null");

		String c = ctx(criteria);
		log.info("{} DELETE start criteria={}", c, criteria);

		try {
			long deleted = repository.deleteByCriteria(criteria);

			log.info("{} DELETE success deletedRows={}", c, deleted);
			return deleted;
		}
		catch (DataIntegrityViolationException e) {
			log.error("{} DELETE integrity violation criteria={}", c, criteria, e);
			throw new ConsistencyViolationException(
					DATA_INTEGRITY_VIOLATION_MSG, e
			);
		}
		catch (Exception e) {
			log.error("{} DELETE failed criteria={}", c, criteria, e);
			throw new PersistenceException(
					"Delete failed criteria=" + criteriaName(criteria), e
			);
		}
	}

	protected UpdateSpec<ENTITY> buildUpdateSpecFromDto(DTO dto) {

		return (update, root) -> {

			Class<?> dtoClass = dto.getClass();

			for (Field f : dtoClass.getDeclaredFields()) {
				f.setAccessible(true);

				if (isIgnoredUpdateField(f)) {
					continue;
				}

				try {
					Object value = f.get(dto);
					if (value == null) {
						continue;
					}

					Path<?> path = resolveEntityPath(root, f.getName());
					if (path == null) {
						continue; // DTO-only field
					}

					@SuppressWarnings("unchecked")
					Path<Object> typedPath = (Path<Object>) path;

					update.set(typedPath, value);
				}
				catch (IllegalAccessException ignored) {
				}
			}
		};
	}

	protected Path<?> resolveEntityPath(
			EntityPathBase<?> root,
			String fieldName) {

		try {
			Field f = root.getClass().getField(fieldName);
			Object v = f.get(root);

			return (v instanceof Path<?> p) ? p : null;
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			return null;
		}
	}

	protected boolean isIgnoredUpdateField(Field f) {
		return Modifier.isStatic(f.getModifiers())
				|| "id".equals(f.getName())
				|| "createdBy".equals(f.getName())
				|| "createdDate".equals(f.getName())
				|| "updatedBy".equals(f.getName())
				|| "updatedDate".equals(f.getName());
	}
}
