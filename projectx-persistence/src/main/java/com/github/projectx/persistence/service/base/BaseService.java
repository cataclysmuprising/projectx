package com.github.projectx.persistence.service.base;

import com.github.projectx.persistence.config.PersistenceContextNames;
import com.github.projectx.persistence.criteria.base.AbstractCriteria;
import com.github.projectx.persistence.dto.base.AbstractDTO;
import com.github.projectx.persistence.dto.base.KeysetResult;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.base.SortItem;
import com.github.projectx.persistence.entity.base.AbstractEntity;
import com.github.projectx.persistence.exception.BusinessException;
import com.github.projectx.persistence.exception.ConsistencyViolationException;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.mapper.base.AbstractMapper;
import com.github.projectx.persistence.mapper.base.MappingContext;
import com.github.projectx.persistence.repository.base.AbstractRepository;
import com.github.projectx.persistence.repository.base.KeysetPage;
import com.github.projectx.persistence.repository.base.UpdateSpec;
import com.github.projectx.persistence.utils.NullUpdatePolicy;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.github.projectx.persistence.utils.LoggerConstants.DATA_INTEGRITY_VIOLATION_MSG;

@Transactional(transactionManager = PersistenceContextNames.TX_MANAGER, rollbackFor = Exception.class)
public abstract class BaseService<
		ID extends Serializable & Comparable<ID>,
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
	private final ConcurrentMap<UpdateBindingKey, List<UpdateFieldBinding>> updateBindingCache =
			new ConcurrentHashMap<>();
	@Autowired
	protected @Nullable EntityManager entityManager;

	@Autowired
	protected @Nullable MappingContext mappingContext;

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
// Common helpers (SHARED)
// ----------------------------------------------------------------------

	protected void assertPositiveId(long value, String fieldName) {
		Assert.isTrue(value > 0, fieldName + " must be positive");
	}

	protected BigDecimal normalizeAmount(BigDecimal amount, String fieldName) {
		Assert.notNull(amount, fieldName + " must not be null");
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

// ----------------------------------------------------------------------
// READ (ENTITY → DTO)
// ----------------------------------------------------------------------

	/**
	 * Encapsulates deterministic data access for `findById` so callers do not duplicate query logic across services.
	 *
	 * @param id input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
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
			Optional<DTO> dto = findByIdInternal(id);

			log.info("{} FIND_BY_ID result found={}", c, dto.isPresent());
			return dto;
		}
		catch (Exception e) {
			log.error("{} FIND_BY_ID failed", c, e);
			throw new PersistenceException("FindById failed id=" + id, e);
		}
	}

	/**
	 * Encapsulates deterministic data access for `findOne` so callers do not duplicate query logic across services.
	 *
	 * @param criteria input required by this operation contract
	 * @param hints    input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Transactional(readOnly = true)
	public Optional<DTO> findOne(CRITERIA criteria, String... hints) throws PersistenceException {

		Assert.notNull(criteria, "Criteria must not be null");

		String c = ctx(criteria);
		log.info("{} FIND_ONE start criteria={} , hints={}", c, criteria, hints);

		try {
			Optional<ENTITY> entity = repository.findOne(criteria, hints);
			boolean includeRelations = hints != null && hints.length > 0;
			assert mappingContext != null;
			Optional<DTO> dto =
					mappingContext.withIncludeRelations(
							includeRelations,
							() -> entity.map(e -> mapper.toDto(e, mappingContext))
					);

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

	/**
	 * Encapsulates deterministic data access for `findAll` so callers do not duplicate query logic across services.
	 *
	 * @param criteria input required by this operation contract
	 * @param hints    input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Transactional(readOnly = true)
	public List<DTO> findAll(CRITERIA criteria, String... hints) throws PersistenceException {

		Assert.notNull(criteria, "Criteria must not be null");

		String c = ctx(criteria);
		log.info("{} FIND_ALL start criteria={}, hints={}", c, criteria, hints);

		try {
			List<ENTITY> entities = repository.findAll(criteria, hints);
			boolean includeRelations = hints != null && hints.length > 0;
			assert mappingContext != null;
			List<DTO> dtos =
					mappingContext.withIncludeRelations(
							includeRelations,
							() -> mapper.mapToDtoList(entities, mappingContext)
					);

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

	/**
	 * Encapsulates deterministic data access for `findByPaging` so callers do not duplicate query logic across services.
	 *
	 * @param criteria input required by this operation contract
	 * @param hints    input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
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

			boolean includeRelations = hints != null && hints.length > 0;
			assert mappingContext != null;
			Page<DTO> dtoPage =
					mappingContext.withIncludeRelations(
							includeRelations,
							() -> page.map(e -> mapper.toDto(e, mappingContext))
					);

			log.info("{} FIND_PAGE success total={} size={}",
					c, dtoPage.getTotalElements(), dtoPage.getNumberOfElements());

			List<DTO> data = dtoPage.getContent();

			List<SortItem> sortItems = new ArrayList<>();
			Sort sort = dtoPage.getSort();

			if (sort.isSorted()) {
				for (Sort.Order o : sort) {
					sortItems.add(
							new SortItem(
									o.getProperty(),
									o.getDirection().name()
							)
					);
				}
			}

			int responsePageNumber = resolveResponsePageNumber(dtoPage);

			return new PaginatedResult<>(
					dtoPage.getTotalElements(),
					dtoPage.getTotalElements(),   // or filtered count if you have it
					dtoPage.getTotalPages(),
					dtoPage.getSize(),
					responsePageNumber,
					dtoPage.getNumberOfElements(),
					sortItems,
					dtoPage.getContent()
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

	private int resolveResponsePageNumber(Page<?> page) {
		if (page == null) {
			return 0;
		}

		int totalPages = page.getTotalPages();
		if (totalPages <= 0) {
			return 0;
		}

		return Math.max(0, Math.min(page.getNumber(), totalPages - 1));
	}

	/**
	 * Cursor-based paging exposed at service level.
	 *
	 * <p>
	 * Why this exists:
	 * <ul>
	 *   <li>Allows API layers to consume keyset pagination without touching repositories.</li>
	 *   <li>Maps entities to DTOs while preserving the repository's deterministic cursor metadata.</li>
	 *   <li>Avoids total-count overhead for infinite scroll and deep traversal use cases.</li>
	 * </ul>
	 * </p>
	 */
	@Transactional(readOnly = true)
	public KeysetResult<DTO, ID> findByKeyset(
			CRITERIA criteria,
			ID afterIdExclusive,
			Integer limit,
			String... hints) throws PersistenceException {

		Assert.notNull(criteria, "Criteria must not be null");

		String c = ctx(criteria);
		log.info(
				"{} FIND_KEYSET start afterIdExclusive={} limit={} criteria={} hints={}",
				c,
				afterIdExclusive,
				limit,
				criteria,
				hints
		);

		try {
			KeysetPage<ENTITY, ID> page =
					repository.findByKeyset(criteria, afterIdExclusive, limit, hints);

			boolean includeRelations = hints != null && hints.length > 0;
			assert mappingContext != null;
			List<DTO> data =
					mappingContext.withIncludeRelations(
							includeRelations,
							() -> mapper.mapToDtoList(page.getData(), mappingContext)
					);

			KeysetResult<DTO, ID> result =
					new KeysetResult<>(
							data,
							page.getNextCursor(),
							page.hasNext(),
							page.getPageSize()
					);

			log.info(
					"{} FIND_KEYSET success size={} hasNext={} nextCursor={}",
					c,
					result.getData().size(),
					result.hasNext(),
					result.getNextCursor()
			);

			return result;
		}
		catch (Exception e) {
			log.error(
					"{} FIND_KEYSET failed afterIdExclusive={} limit={} criteria={} hints={}",
					c,
					afterIdExclusive,
					limit,
					criteria,
					hints,
					e
			);
			throw new PersistenceException(
					"FindKeyset failed criteria=" + criteriaName(criteria),
					e
			);
		}
	}

	/**
	 * Encapsulates deterministic data access for `existsById` so callers do not duplicate query logic across services.
	 *
	 * @param id input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Transactional(readOnly = true)
	public boolean existsById(ID id) throws PersistenceException {

		Assert.notNull(id, "Id must not be null");

		log.info("{} EXISTS_BY_ID start id={}", id, id);

		try {
			boolean exists = repository.existsById(id);
			log.info("{} EXISTS_BY_ID result={} id={}", id, exists, id);
			return exists;
		}
		catch (Exception e) {
			log.error("{} EXISTS_BY_ID failed id={}", id, id, e);
			throw new PersistenceException(
					"ExistsById failed id=" + id, e
			);
		}
	}

	/**
	 * Encapsulates deterministic data access for `exists` so callers do not duplicate query logic across services.
	 *
	 * @param criteria input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
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

	/**
	 * Encapsulates deterministic data access for `count` so callers do not duplicate query logic across services.
	 *
	 * @param criteria input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
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

	/**
	 * Encapsulates deterministic data access for `create` so callers do not duplicate query logic across services.
	 *
	 * @param dto       input required by this operation contract
	 * @param createdBy input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	public DTO create(DTO dto, long createdBy) throws PersistenceException, ConsistencyViolationException, BusinessException {

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

	/**
	 * Encapsulates deterministic data access for `createAll` so callers do not duplicate query logic across services.
	 *
	 * @param dtos      input required by this operation contract
	 * @param createdBy input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
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

	/**
	 * Encapsulates deterministic data access for `update` so callers do not duplicate query logic across services.
	 *
	 * @param dto       input required by this operation contract
	 * @param root      input required by this operation contract
	 * @param updatedBy input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	public DTO update(
			DTO dto,
			EntityPathBase<ENTITY> root,
			long updatedBy
	) throws PersistenceException, ConsistencyViolationException {

		return update(dto, root, updatedBy, NullUpdatePolicy.IGNORE_NULLS);
	}

	/**
	 * Encapsulates deterministic data access for `update` so callers do not duplicate query logic across services.
	 *
	 * @param dto        input required by this operation contract
	 * @param root       input required by this operation contract
	 * @param updatedBy  input required by this operation contract
	 * @param nullPolicy input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	public DTO update(
			DTO dto,
			EntityPathBase<ENTITY> root,
			long updatedBy,
			NullUpdatePolicy nullPolicy
	) throws PersistenceException, ConsistencyViolationException {

		Assert.notNull(dto, "DTO must not be null");
		Assert.notNull(dto.getId(), "DTO id must not be null");

		@SuppressWarnings("unchecked")
		ID id = (ID) dto.getId();

		String c = String.format(
				"[service=%s][dto=%s][id=%s][nullPolicy=%s]",
				serviceName(),
				dto.getClass().getSimpleName(),
				id,
				nullPolicy
		);

		log.info("{} UPDATE_BY_ID start updatedBy={}", c, updatedBy);

		try {
			UpdateSpec<ENTITY> spec =
					buildUpdateSpecFromDto(dto, root, nullPolicy);

			long affected = repository.updateById(spec, id, updatedBy);

			if (affected == 0) {
				throw new EntityNotFoundException(
						"Entity not found for id=" + id
				);
			}

			return findByIdInternal(id).orElseThrow(
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

	private Optional<DTO> findByIdInternal(ID id) {
		Optional<ENTITY> entity = repository.findById(id);
		assert mappingContext != null;
		return mappingContext.withIncludeRelations(
				false,
				() -> entity.map(e -> mapper.toDto(e, mappingContext))
		);
	}

	/**
	 * Encapsulates deterministic data access for `update` so callers do not duplicate query logic across services.
	 *
	 * @param spec      input required by this operation contract
	 * @param criteria  input required by this operation contract
	 * @param updatedBy input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
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

	/**
	 * Encapsulates deterministic data access for `deleteById` so callers do not duplicate query logic across services.
	 *
	 * @param id input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	public boolean deleteById(ID id) throws PersistenceException {
		Assert.notNull(id, "Id must not be null");

		String c = String.format(
				"[service=%s][domain=%s][id=%s]",
				serviceName(),
				"DeleteById",
				id
		);

		log.info("{} DELETE_BY_ID start", c);

		try {
			boolean deleted = repository.deleteWithId(id);
			log.info("{} DELETE_BY_ID result={}", c, deleted);
			return deleted;
		}
		catch (Exception e) {
			log.error("{} DELETE_BY_ID failed", c, e);
			throw new PersistenceException("DeleteById failed id=" + id, e);
		}
	}

	/**
	 * Encapsulates deterministic data access for `delete` so callers do not duplicate query logic across services.
	 *
	 * @param criteria input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
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

	protected <D, E extends AbstractEntity>
	UpdateSpec<E> buildUpdateSpecFromDto(
			D dto,
			EntityPathBase<E> root
	) {
		return buildUpdateSpecFromDto(dto, root, NullUpdatePolicy.IGNORE_NULLS);
	}

	protected <D, E extends AbstractEntity>
	UpdateSpec<E> buildUpdateSpecFromDto(
			D dto,
			EntityPathBase<E> root,
			NullUpdatePolicy nullPolicy
	) {
		List<UpdateFieldBinding> bindings = resolveUpdateBindings(dto.getClass(), root);
		return (update, ignoredRoot) -> {
			for (UpdateFieldBinding binding : bindings) {
				try {
					Object value = binding.field().get(dto);

					if (value == null) {

						if (nullPolicy == NullUpdatePolicy.IGNORE_NULLS) {
							continue;
						}

						if (binding.skipNullUpdate()) {
							continue;
						}

						update.set(binding.path(), (Object) null);
						continue;
					}

					if (!isUpdatableValue(value)) {
						continue;
					}

					update.set(binding.path(), value);
				}
				catch (IllegalAccessException e) {
					throw new IllegalStateException(
							"Failed to resolve DTO field value for update field=" + binding.field().getName(),
							e
					);
				}
			}
		};
	}

	private List<UpdateFieldBinding> resolveUpdateBindings(
			Class<?> dtoClass,
			EntityPathBase<?> root
	) {
		UpdateBindingKey key = new UpdateBindingKey(dtoClass, root.getClass());
		return updateBindingCache.computeIfAbsent(key, ignored -> buildUpdateBindings(dtoClass, root));
	}

	private List<UpdateFieldBinding> buildUpdateBindings(
			Class<?> dtoClass,
			EntityPathBase<?> root
	) {
		Map<String, UpdateFieldBinding> bindings = new LinkedHashMap<>();
		for (Field field : getCandidateUpdateFields(dtoClass)) {
			if (isIgnoredUpdateField(field)) {
				continue;
			}

			Path<?> path = resolveEntityPath(root, field.getName());
			if (path == null || isRelationPath(path) || !isScalarPath(path)) {
				continue;
			}

			String propertyName = path.getMetadata().getName();
			if (bindings.containsKey(propertyName)) {
				continue;
			}

			field.setAccessible(true);

			@SuppressWarnings("unchecked")
			Path<Object> typedPath = (Path<Object>) path;

			bindings.put(
					propertyName,
					new UpdateFieldBinding(
							field,
							typedPath,
							isNonNullableForeignKey(root, field.getName())
					)
			);
		}
		return List.copyOf(bindings.values());
	}

	private List<Field> getCandidateUpdateFields(Class<?> dtoClass) {
		List<Field> fields = new ArrayList<>();
		Class<?> current = dtoClass;
		while (current != null && current != Object.class) {
			fields.addAll(Arrays.asList(current.getDeclaredFields()));
			current = current.getSuperclass();
		}
		return fields;
	}

	protected Path<?> resolveEntityPath(
			EntityPathBase<?> root,
			String fieldName) {

		try {
			Field f = root.getClass().getField(fieldName);
			Object v = f.get(root);

			return (v instanceof Path<?> p) ? p : null;
		}
		catch (NoSuchFieldException e) {
			return null;
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException("Failed to resolve entity path field=" + fieldName, e);
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

	protected boolean isUpdatableValue(Object value) {
		return value instanceof String
				|| value instanceof Number
				|| value instanceof Boolean
				|| value instanceof Enum<?>
				|| value instanceof java.time.temporal.Temporal
				|| value instanceof Date;
	}

	protected boolean isRelationPath(Path<?> path) {
		// EntityPathBase covers @ManyToOne / @OneToOne etc.
		// CollectionPathBase covers @OneToMany / @ManyToMany etc.
		return (path instanceof EntityPathBase)
				|| (path instanceof com.querydsl.core.types.dsl.CollectionPathBase);
	}

	protected boolean isScalarPath(Path<?> path) {
		// Accept only simple leaf paths.
		// This intentionally rejects EntityPathBase and collections.
		return (path instanceof com.querydsl.core.types.dsl.StringPath)
				|| (path instanceof com.querydsl.core.types.dsl.NumberPath)
				|| (path instanceof com.querydsl.core.types.dsl.EnumPath)
				|| (path instanceof com.querydsl.core.types.dsl.BooleanPath)
				|| (path instanceof com.querydsl.core.types.dsl.DatePath)
				|| (path instanceof com.querydsl.core.types.dsl.DateTimePath)
				|| (path instanceof com.querydsl.core.types.dsl.TimePath)
				|| (path instanceof com.querydsl.core.types.dsl.SimplePath);
	}

	protected boolean isNonNullableForeignKey(
			EntityPathBase<?> root,
			String fieldName
	) {

		Class<?> entityClass = root.getType();

		assert entityManager != null;
		Metamodel metamodel = entityManager.getMetamodel();
		EntityType<?> entityType = metamodel.entity(entityClass);

		Attribute<?, ?> attr;
		try {
			attr = entityType.getAttribute(fieldName);
		}
		catch (IllegalArgumentException notFound) {
			return false; // DTO field not part of entity mapping
		}

		if (!(attr instanceof SingularAttribute<?, ?> sa)) {
			return false; // collections etc.
		}

		// 1) Association FK: @ManyToOne / @OneToOne
		if (sa.isAssociation()) {
			// Prefer @JoinColumn(nullable=...) if present (most accurate for FK column)
			java.lang.reflect.Member member = sa.getJavaMember();
			jakarta.persistence.JoinColumn jc = getAnnotationFromMember(member, jakarta.persistence.JoinColumn.class);
			if (jc != null) {
				return !jc.nullable();
			}

			// Fallback: JPA optional flag (works when optional=false was used)
			return !sa.isOptional();
		}

		// 2) Scalar FK column: userId, parentId, etc.
		java.lang.reflect.Member member = sa.getJavaMember();
		jakarta.persistence.Column col = getAnnotationFromMember(member, jakarta.persistence.Column.class);
		if (col != null) {
			return !col.nullable();
		}

		// If no @Column present, assume it's nullable (safe default)
		return false;
	}

	private <A extends java.lang.annotation.Annotation> A getAnnotationFromMember(
			java.lang.reflect.Member member,
			Class<A> annotationType
	) {
		if (member instanceof Field f) {
			return f.getAnnotation(annotationType);
		}
		if (member instanceof java.lang.reflect.Method m) {
			return m.getAnnotation(annotationType);
		}
		return null;
	}

	protected <T> void applyUpdateValue(
			JPAUpdateClause update,
			Path<T> path,
			T value,
			NullUpdatePolicy nullPolicy
	) {
		Assert.notNull(update, "Update clause must not be null");
		Assert.notNull(path, "Path must not be null");
		Assert.notNull(nullPolicy, "NullUpdatePolicy must not be null");

		if (value == null && nullPolicy == NullUpdatePolicy.IGNORE_NULLS) {
			return;
		}
		update.set(path, value);
	}

	private record UpdateBindingKey(Class<?> dtoClass, Class<?> rootClass) {
	}

	private record UpdateFieldBinding(
			Field field,
			Path<Object> path,
			boolean skipNullUpdate
	) {
	}
}

