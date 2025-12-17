package com.tamantaw.projectx.persistence.repository.base;

import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.AbstractJPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import com.tamantaw.projectx.persistence.criteria.base.AbstractCriteria;
import com.tamantaw.projectx.persistence.entity.base.AbstractEntity;
import com.tamantaw.projectx.persistence.entity.base.QAbstractEntity;
import jakarta.persistence.EntityManager;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.*;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Base JPA repository implementation with:
 * <p>
 * - QueryDSL-based dynamic criteria
 * - N+1-safe fetching
 * - Pagination-safe ordering
 * - Deterministic results across databases
 * - Explicit API contracts
 *
 * <p>
 * This class intentionally avoids “magic behavior”.
 * Every performance-critical decision is explicit and documented.
 * </p>
 */
public abstract class AbstractRepositoryImpl<
		ENTITY extends AbstractEntity,
		QCLAZZ extends EntityPathBase<ENTITY>,
		CRITERIA extends AbstractCriteria<QCLAZZ>,
		ID extends Serializable>
		extends SimpleJpaRepository<ENTITY, ID>
		implements AbstractRepository<ENTITY, QCLAZZ, CRITERIA, ID> {

	// ----------------------------------------------------------------------
	// STATIC CONFIGURATION
	// ----------------------------------------------------------------------

	private static final EntityPathResolver PATH_RESOLVER =
			SimpleEntityPathResolver.INSTANCE;

	private static final Logger logger =
			LogManager.getLogger("repositoryLogs." + AbstractRepositoryImpl.class.getName());

	/**
	 * Indicates whether the underlying database is PostgreSQL.
	 *
	 * <p><b>Why this flag exists</b></p>
	 * <ul>
	 *   <li>
	 *     PostgreSQL guarantees stable ORDER BY semantics when reapplying
	 *     sorting after an ID-based paging query.
	 *   </li>
	 *   <li>
	 *     PostgreSQL optimizes OFFSET / LIMIT efficiently even on complex queries.
	 *   </li>
	 *   <li>
	 *     Other databases do NOT guarantee order preservation for
	 *     <code>IN (...)</code> queries and therefore require explicit ordering.
	 *   </li>
	 * </ul>
	 *
	 * <p><b>Design decision</b></p>
	 * <ul>
	 *   <li>We do NOT auto-detect the DB dialect.</li>
	 *   <li>Auto-detection hides behavior and may change after upgrades.</li>
	 *   <li>This flag makes pagination behavior explicit and reviewable.</li>
	 * </ul>
	 *
	 * <p>
	 * ⚠️ Changing this flag affects SQL generation and must be reviewed
	 * together with DB choice and indexing strategy.
	 * </p>
	 */
	public static final boolean IS_POSTGRES_DB = true;

	// ----------------------------------------------------------------------
	// CORE FIELDS
	// ----------------------------------------------------------------------

	protected final EntityManager entityManager;
	protected final QCLAZZ path;
	protected final QAbstractEntity audit;
	protected final Querydsl querydsl;
	protected final JPAQueryFactory queryFactory;

	// ----------------------------------------------------------------------
	// CONSTRUCTOR
	// ----------------------------------------------------------------------

	protected AbstractRepositoryImpl(
			Class<ENTITY> domainClass,
			EntityManager entityManager) {

		super(
				new JpaMetamodelEntityInformation<>(
						domainClass,
						entityManager.getMetamodel(),
						entityManager.getEntityManagerFactory()
								.getPersistenceUnitUtil()
				),
				entityManager
		);

		this.entityManager = entityManager;

		// Resolve QueryDSL root path (QEntity)
		@SuppressWarnings("unchecked")
		QCLAZZ resolved = (QCLAZZ) PATH_RESOLVER.createPath(domainClass);
		path = resolved;

		// PathBuilder is required for dynamic ORDER BY
		PathBuilder<ENTITY> builder =
				new PathBuilder<>(path.getType(), path.getMetadata());

		querydsl = new Querydsl(entityManager, builder);
		queryFactory = new JPAQueryFactory(entityManager);

		// Resolve audit (_super) fields such as id, createdDate, etc.
		audit = resolveAuditPath(path);
	}

	// ----------------------------------------------------------------------
	// READ OPERATIONS
	// ----------------------------------------------------------------------

	/**
	 * Simple primary-key lookup.
	 */
	@Override
	@Nonnull
	public Optional<ENTITY> findById(@Nonnull ID id) {
		Assert.notNull(id, "Id must not be null");
		return super.findById(id);
	}

	/**
	 * Returns exactly one entity or empty.
	 *
	 * <p>
	 * The criteria MUST be logically unique.
	 * If more than one row matches, an exception is thrown.
	 * </p>
	 */
	@Override
	public Optional<ENTITY> findOne(CRITERIA criteria, String... hints) {

		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		JPQLQuery<ENTITY> query =
				createQuery(filter, hints).select(path);

		// Enforce deterministic ordering even for single-row queries
		applySort(query, criteria);

		List<ENTITY> rows = query.limit(2).fetch();

		if (rows.isEmpty()) {
			return Optional.empty();
		}

		if (rows.size() > 1) {
			throw new IllegalStateException(
					"findOne() returned more than one result"
			);
		}

		return Optional.of(rows.getFirst());
	}

	/**
	 * Returns all matching entities without pagination.
	 *
	 * <p>
	 * Paging is NOT allowed here to prevent unsafe joins + pagination bugs.
	 * </p>
	 */
	@Override
	public List<ENTITY> findAll(CRITERIA criteria, String... hints) {

		Assert.notNull(criteria, "Criteria must not be null");

		// Enforce correct API usage
		if (criteria.toPageable() != null) {
			throw new IllegalStateException(
					"Paging is not supported in findAll(). Use findByPaging()."
			);
		}

		Predicate filter = criteria.getFilter(path);

		JPQLQuery<ENTITY> query =
				createQuery(filter, hints).select(path);

		applySort(query, criteria);

		return query.fetch();
	}

	/**
	 * Safe pagination method.
	 *
	 * <p>
	 * Guarantees:
	 * <ul>
	 *   <li>Correct offset + limit behavior</li>
	 *   <li>No in-memory pagination</li>
	 *   <li>No duplicate rows</li>
	 *   <li>Stable ordering</li>
	 * </ul>
	 * </p>
	 */
	@Override
	public Page<ENTITY> findByPaging(CRITERIA criteria, String... hints) {

		Assert.notNull(criteria, "Criteria must not be null");

		Pageable pageable = criteria.toPageable();
		Assert.notNull(pageable, "Pageable must not be null");

		Predicate filter = criteria.getFilter(path);

		if (!IS_POSTGRES_DB && pageable.getPageSize() > 100) {
			logger.warn(
					"Large page size with non-Postgres DB may cause heavy SQL"
			);
		}

		boolean requiresIdFirst =
				!fetchGraphIsToOneOnly(hints);

		// ------------------------------------------------------------
		// FAST PATH (no collection fetch)
		// ------------------------------------------------------------
		if (!requiresIdFirst) {

			JPQLQuery<ENTITY> query =
					createQuery(filter, hints).select(path);

			query = querydsl.applyPagination(pageable, query);

			List<ENTITY> content = query.fetch();
			long total = count(criteria);

			return new PageImpl<>(content, pageable, total);
		}

		// ------------------------------------------------------------
		// PHASE 1 — ID PAGE (GLOBAL ORDER + OFFSET/LIMIT)
		// ------------------------------------------------------------
		JPQLQuery<Long> idQuery =
				createQuery(filter).select(audit.id);

		idQuery = querydsl.applyPagination(pageable, idQuery);

		List<Long> ids = idQuery.fetch();

		if (ids.isEmpty()) {
			return Page.empty(pageable);
		}

		// ------------------------------------------------------------
		// PHASE 2 — ENTITY FETCH
		// ------------------------------------------------------------
		JPQLQuery<ENTITY> entityQuery =
				createQuery(audit.id.in(ids), hints).select(path);

		if (IS_POSTGRES_DB) {
			// PostgreSQL: reapply ORDER BY safely
			applySort(entityQuery, criteria);
		}
		else {
			// Generic DB: preserve phase-1 order explicitly
			applyIdOrder(entityQuery, ids);
		}

		List<ENTITY> content = entityQuery.fetch();
		long total = count(criteria);

		return new PageImpl<>(content, pageable, total);
	}

	// ----------------------------------------------------------------------
	// ID / COUNT / EXISTS
	// ----------------------------------------------------------------------

	/**
	 * Returns matching entity IDs.
	 *
	 * <p>
	 * Used internally for bulk operations.
	 * Ordering does NOT affect correctness here.
	 * </p>
	 */
	@Override
	public List<Long> findIds(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		JPQLQuery<Long> query =
				createQuery(filter).select(audit.id);

		applySort(query, criteria);

		return query.fetch();
	}

	/**
	 * Count query with identical filter but no joins or pagination.
	 */
	@Override
	public long count(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		Long count =
				createQuery(filter)
						.select(audit.id.count())
						.fetchOne();

		return count == null ? 0L : count;
	}

	/**
	 * Existence check.
	 */
	@Override
	public boolean exists(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		return createQuery(filter)
				.select(audit.id)
				.fetchFirst() != null;
	}

	// ----------------------------------------------------------------------
	// WRITE OPERATIONS
	// ----------------------------------------------------------------------

	@Override
	public ENTITY saveRecord(ENTITY entity) {
		return super.saveAndFlush(entity);
	}

	@Override
	public List<ENTITY> saveAllRecords(Iterable<ENTITY> entities) {
		return super.saveAllAndFlush(entities);
	}

	// ----------------------------------------------------------------------
	// BULK OPERATIONS (ID-FIRST, SAFE)
	// ----------------------------------------------------------------------

	@Override
	public <E extends ENTITY> long updateByCriteria(
			UpdateSpec<E> spec,
			CRITERIA criteria,
			Long updatedBy) {

		Assert.notNull(criteria, "Criteria must not be null");

		@SuppressWarnings("unchecked")
		EntityPathBase<E> typedPath =
				(EntityPathBase<E>) path;

		List<Long> ids = findIds(criteria);
		if (ids.isEmpty()) {
			return 0;
		}

		long affected = 0;

		for (List<Long> chunk : chunk(ids, bulkInChunkSize())) {

			JPAUpdateClause update =
					queryFactory.update(typedPath);

			applyAudit(update, updatedBy);
			spec.apply(update, typedPath);

			affected +=
					update.where(audit.id.in(chunk)).execute();
		}

		return affected;
	}

	@Override
	public long deleteByCriteria(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		List<Long> ids = findIds(criteria);
		if (ids.isEmpty()) {
			return 0;
		}

		long affected = 0;

		for (List<Long> chunk : chunk(ids, bulkInChunkSize())) {
			affected += queryFactory
					.delete(path)
					.where(audit.id.in(chunk))
					.execute();
		}

		return affected;
	}

	// ----------------------------------------------------------------------
	// INTERNAL HELPERS
	// ----------------------------------------------------------------------

	protected AbstractJPAQuery<?, ?> createQuery(
			Predicate predicate,
			String... hints) {

		AbstractJPAQuery<?, ?> query =
				querydsl.createQuery(path);

		if (predicate != null) {
			query.where(predicate);
		}

		QueryHints qh = getRelatedDataHints(hints);
		if (qh != null) {
			qh.forEach(query::setHint);
		}

		return query;
	}

	protected void applyAudit(
			JPAUpdateClause update,
			Long updatedBy) {

		update.set(audit.updatedDate, LocalDateTime.now());
		update.set(audit.updatedBy, updatedBy);
	}

	/**
	 * Applies dynamic ORDER BY based on criteria.
	 */
	protected void applySort(JPQLQuery<?> query, CRITERIA criteria) {

		Sort sort = criteria.resolveSort();
		if (sort == null || !sort.isSorted()) {
			return;
		}

		PathBuilder<?> pb =
				new PathBuilder<>(path.getType(), path.getMetadata());

		for (Sort.Order o : sort) {
			try {
				ComparableExpressionBase<?> exp =
						pb.getComparable(o.getProperty(), Comparable.class);

				query.orderBy(
						o.isAscending() ? exp.asc() : exp.desc()
				);
			}
			catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException(
						"Invalid sort property '" + o.getProperty() + "' for entity "
								+ path.getType().getSimpleName()
								+ ". Ensure the field exists and is Comparable.",
						ex
				);
			}
		}
	}

	protected int bulkInChunkSize() {
		return 1000;
	}

	/**
	 * Preserves ID order explicitly using CASE expressions.
	 *
	 * <p>
	 * Used only for non-Postgres databases.
	 * Page size bounds the SQL complexity.
	 * </p>
	 */
	protected void applyIdOrder(
			JPQLQuery<?> query,
			List<Long> ids) {

		if (ids == null || ids.isEmpty()) {
			return;
		}

		CaseBuilder cb = new CaseBuilder();
		CaseBuilder.Cases<Integer, NumberExpression<Integer>> cases = null;

		int index = 0;
		for (Long id : ids) {
			if (cases == null) {
				cases = cb.when(audit.id.eq(id)).then(index++);
			}
			else {
				cases = cases.when(audit.id.eq(id)).then(index++);
			}
		}

		query.orderBy(
				cases.otherwise(Integer.MAX_VALUE).asc()
		);
	}

	protected boolean fetchGraphIsToOneOnly(String... hints) {
		if (hints == null) {
			return true;
		}
		for (String hint : hints) {
			if (hint.contains("(")) {
				return false;
			}
		}
		return true;
	}

	protected static QueryHints getRelatedDataHints(String... hints) {
		if (ArrayUtils.isNotEmpty(hints)) {
			MutableQueryHints qh = new MutableQueryHints();
			qh.add("jakarta.persistence.fetchgraph", String.join(",", hints));
			return qh;
		}
		return null;
	}

	private QAbstractEntity resolveAuditPath(EntityPath<?> p) {

		if (p instanceof QAbstractEntity qa) {
			return qa;
		}

		if (p instanceof EntityPathBase<?> epb) {
			try {
				Field f = epb.getClass().getField("_super");
				return (QAbstractEntity) f.get(epb);
			}
			catch (Exception ignored) {
			}
		}

		throw new IllegalStateException(
				"QAbstractEntity audit path not resolvable"
		);
	}

	private static <T> List<List<T>> chunk(
			List<T> src,
			int size) {

		if (src == null || src.isEmpty()) {
			return List.of();
		}

		List<List<T>> out =
				new ArrayList<>((src.size() + size - 1) / size);

		for (int i = 0; i < src.size(); i += size) {
			out.add(src.subList(i, Math.min(src.size(), i + size)));
		}

		return out;
	}
}

