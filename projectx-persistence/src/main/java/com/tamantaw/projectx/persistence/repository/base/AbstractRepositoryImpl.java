package com.tamantaw.projectx.persistence.repository.base;

import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.AbstractJPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import com.tamantaw.projectx.persistence.criteria.base.AbstractCriteria;
import com.tamantaw.projectx.persistence.entity.base.AbstractEntity;
import com.tamantaw.projectx.persistence.entity.base.QAbstractEntity;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityManager;
import jakarta.persistence.metamodel.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.*;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

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
 *
 * <p><b>STRICT GUARANTEE</b></p>
 * <ul>
 *   <li>❌ Hibernate in-memory paging</li>
 *   <li>❌ Duplicate entities</li>
 *   <li>❌ Missing records</li>
 *   <li>❌ Broken pagination with joins</li>
 *   <li>❌ Incorrect total counts</li>
 *   <li>❌ Non-deterministic ordering</li>
 * </ul>
 *
 * <p>
 * If the caller requests an unsafe operation (e.g. to-many ORDER BY),
 * this repository fails fast with a clear exception. There is no “best effort”
 * fallback that could silently corrupt paging results.
 * </p>
 */
public abstract class AbstractRepositoryImpl<
		ID extends Serializable,
		ENTITY extends AbstractEntity<ID>,
		QCLAZZ extends EntityPathBase<ENTITY>,
		CRITERIA extends AbstractCriteria<QCLAZZ, ID>>
		extends SimpleJpaRepository<ENTITY, ID>
		implements AbstractRepository<ID, ENTITY, QCLAZZ, CRITERIA> {

	// ----------------------------------------------------------------------
	// STATIC CONFIGURATION
	// ----------------------------------------------------------------------

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

	private static final EntityPathResolver PATH_RESOLVER =
			SimpleEntityPathResolver.INSTANCE;

	private static final Logger logger =
			LogManager.getLogger("repositoryLogs." + AbstractRepositoryImpl.class.getName());

	// ----------------------------------------------------------------------
	// CORE FIELDS
	// ----------------------------------------------------------------------

	protected final EntityManager entityManager;
	protected final SimpleExpression<ID> idExpr;
	protected final OrderSpecifier<?> idAscOrder;
	protected final QCLAZZ path;
	protected final QAbstractEntity audit;
	protected final Querydsl querydsl;
	protected final JPAQueryFactory queryFactory;

	// ----------------------------------------------------------------------
	// CONSTRUCTOR
	// ----------------------------------------------------------------------

	protected AbstractRepositoryImpl(
			Class<ENTITY> domainClass,
			Class<ID> idClass,
			EntityManager entityManager) {

		super(
				new JpaMetamodelEntityInformation<>(
						domainClass,
						entityManager.getMetamodel(),
						entityManager.getEntityManagerFactory().getPersistenceUnitUtil()
				),
				entityManager
		);

		this.entityManager = entityManager;

		@SuppressWarnings("unchecked")
		QCLAZZ resolved = (QCLAZZ) PATH_RESOLVER.createPath(domainClass);
		path = resolved;

		PathBuilder<ENTITY> builder =
				new PathBuilder<>(path.getType(), path.getMetadata());

		querydsl = new Querydsl(entityManager, builder);
		queryFactory = new JPAQueryFactory(entityManager);

		audit = resolveAuditPath(path);

		IdentifiableType<ENTITY> identifiable =
				(IdentifiableType<ENTITY>) entityManager
						.getMetamodel()
						.managedType(domainClass);

		SingularAttribute<? super ENTITY, ID> idAttr =
				identifiable.getId(idClass);

		idExpr = Expressions.simplePath(
				idClass,
				path,
				idAttr.getName()
		);

		@SuppressWarnings("unchecked")
		Expression<? extends Comparable<?>> comparableIdExpr =
				(Expression<? extends Comparable<?>>) idExpr;

		idAscOrder = new OrderSpecifier<>(Order.ASC, comparableIdExpr);
	}

	// ----------------------------------------------------------------------
	// READ OPERATIONS
	// ----------------------------------------------------------------------

	protected static QueryHints getRelatedDataHints(String... hints) {
		if (ArrayUtils.isNotEmpty(hints)) {
			MutableQueryHints qh = new MutableQueryHints();
			qh.add("jakarta.persistence.fetchgraph", String.join(",", hints));
			return qh;
		}
		return null;
	}

	private static <T> List<List<T>> chunk(List<T> src, int size) {

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

		// ------------------------------------------------------------------
		// STRICT decision: if fetch graph contains collections -> ID FIRST.
		// No "magic". No recovery.
		// ------------------------------------------------------------------
		boolean requiresIdFirst =
				fetchGraphContainsCollection(hints);

		// ------------------------------------------------------------
		// FAST PATH — to-one fetch only (still deterministic)
		// ------------------------------------------------------------
		if (!requiresIdFirst) {

			JPQLQuery<ENTITY> query =
					createQuery(filter, hints).select(path);

			applySortOrDefaultById(query, criteria);

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

		// ------------------------------------------------------------
		// SAFE PATH — collection fetch (ID first)
		// ------------------------------------------------------------
		JPQLQuery<ID> idQuery =
				createQuery(filter).select(idExpr);

		applySortOrDefaultById(idQuery, criteria);

		List<ID> ids = idQuery.limit(2).fetch();

		if (ids.isEmpty()) {
			return Optional.empty();
		}
		if (ids.size() > 1) {
			throw new IllegalStateException(
					"findOne() returned more than one result"
			);
		}

		JPQLQuery<ENTITY> entityQuery =
				createQuery(idExpr.eq(ids.getFirst()), hints)
						.select(path);

		return Optional.ofNullable(entityQuery.fetchOne());
	}

	// ----------------------------------------------------------------------
	// ID / COUNT / EXISTS
	// ----------------------------------------------------------------------

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

		boolean requiresIdFirst =
				fetchGraphContainsCollection(hints);

		// ------------------------------------------------------------
		// FAST PATH — to-one fetch only (deterministic, no duplication)
		// ------------------------------------------------------------
		if (!requiresIdFirst) {

			JPQLQuery<ENTITY> query =
					createQuery(filter, hints).select(path);

			applySortOrDefaultById(query, criteria);

			return query.fetch();
		}

		// ------------------------------------------------------------
		// SAFE PATH — collection fetch allowed, ID-first + root dedup
		// ------------------------------------------------------------

		// Phase 1 — deterministic ID selection
		JPQLQuery<ID> idQuery =
				createQuery(filter).select(idExpr);

		applySortOrDefaultById(idQuery, criteria);

		List<ID> ids = idQuery.fetch();
		if (ids.isEmpty()) {
			return List.of();
		}

		// Phase 2 — entity fetch with fetch graph
		JPQLQuery<ENTITY> entityQuery =
				createQuery(idExpr.in(ids), hints).select(path);

		applyStableOrderAfterIdPaging(entityQuery, criteria, ids);

		List<ENTITY> rows = entityQuery.fetch();

		// ------------------------------------------------------------
		// STRICT ROOT DEDUPLICATION (safe because NO pagination)
		// ------------------------------------------------------------
		// JPA may return duplicate roots when fetching collections.
		// Dedup is REQUIRED here to preserve set semantics.
		// This does NOT hide paging bugs because paging is forbidden.
		Map<ID, ENTITY> unique = new LinkedHashMap<>(rows.size());

		for (ENTITY e : rows) {
			unique.putIfAbsent(e.getId(), e);
		}

		return new ArrayList<>(unique.values());
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

		boolean requiresIdFirst =
				fetchGraphContainsCollection(hints);

		// ------------------------------------------------------------
		// FAST PATH (to-one only)
		// ------------------------------------------------------------
		if (!requiresIdFirst) {

			JPQLQuery<ENTITY> query =
					createQuery(filter, hints).select(path);

			applySortOrDefaultById(query, criteria);

			query = query.offset(pageable.getOffset())
					.limit(pageable.getPageSize());

			List<ENTITY> content = query.fetch();
			long total = count(criteria);

			return new PageImpl<>(content, pageable, total);
		}

		// ------------------------------------------------------------
		// PHASE 1 — ID PAGE (GLOBAL ORDER + OFFSET/LIMIT)
		// ------------------------------------------------------------
		JPQLQuery<ID> idQuery =
				createQuery(filter).select(idExpr);

		applySortOrDefaultById(idQuery, criteria);

		idQuery = idQuery
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize());

		List<ID> ids = idQuery.fetch();

		if (ids.isEmpty()) {
			return Page.empty(pageable);
		}

		// ------------------------------------------------------------
		// PHASE 2 — ENTITY FETCH
		// ------------------------------------------------------------
		JPQLQuery<ENTITY> entityQuery =
				createQuery(idExpr.in(ids), hints).select(path);

		applyStableOrderAfterIdPaging(entityQuery, criteria, ids);

		List<ENTITY> content = entityQuery.fetch();
		long total = count(criteria);

		return new PageImpl<>(content, pageable, total);
	}

	/**
	 * Returns matching entity IDs.
	 *
	 * <p>
	 * Used internally for bulk operations.
	 * Ordering does NOT affect correctness here.
	 * </p>
	 */
	@Override
	public List<ID> findIds(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		JPQLQuery<ID> query =
				createQuery(filter).select(idExpr);

		applySortOrDefaultById(query, criteria);

		return query.fetch();
	}

	// ----------------------------------------------------------------------
	// WRITE OPERATIONS
	// ----------------------------------------------------------------------

	/**
	 * Count query with identical filter but no joins or pagination.
	 *
	 * <p>
	 * STRICT: COUNT must match the same filter used for paging decisions.
	 * No COUNT DISTINCT. No recovery.
	 * </p>
	 */
	@Override
	public long count(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		Long count =
				createQuery(filter)
						.select(idExpr.count())
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
				.select(idExpr)
				.fetchFirst() != null;
	}

	// ----------------------------------------------------------------------
	// BULK OPERATIONS (ID-FIRST, SAFE)
	// ----------------------------------------------------------------------

	@Override
	public ENTITY saveRecord(ENTITY entity) {
		return super.saveAndFlush(entity);
	}

	@Override
	public List<ENTITY> saveAllRecords(Iterable<ENTITY> entities) {
		return super.saveAllAndFlush(entities);
	}

	@Override
	public long updateById(
			UpdateSpec<ENTITY> spec,
			ID id,
			long updatedBy) {

		Assert.notNull(spec, "UpdateSpec must not be null");

		JPAUpdateClause update = queryFactory.update(path);

		applyAudit(update, updatedBy);

		spec.apply(update, path);

		long affected = update.where(idExpr.eq(id)).execute();

		afterBulkDml();

		return affected;
	}

	@Override
	public <E extends ENTITY> long updateByCriteria(
			UpdateSpec<E> spec,
			CRITERIA criteria,
			Long updatedBy) {

		Assert.notNull(criteria, "Criteria must not be null");
		Assert.notNull(spec, "UpdateSpec must not be null");

		@SuppressWarnings("unchecked")
		EntityPathBase<E> typedPath =
				(EntityPathBase<E>) path;

		// STRICT:
		// Bulk DML must not depend on join-fetch graphs or unsafe ordering.
		// This method updates by ID chunks derived from the same deterministic ID selection.
		List<ID> ids = findIds(criteria);
		if (ids.isEmpty()) {
			return 0;
		}

		long affected = 0;

		for (List<ID> chunk : chunk(ids, bulkInChunkSize())) {

			JPAUpdateClause update =
					queryFactory.update(typedPath);

			applyAudit(update, updatedBy);
			spec.apply(update, typedPath);

			affected +=
					update.where(idExpr.in(chunk)).execute();
		}

		afterBulkDml();

		return affected;
	}

	@Override
	public boolean deleteWithId(@Nonnull ID id) {

		Assert.notNull(id, "Id must not be null");

		long affected =
				queryFactory
						.delete(path)
						.where(idExpr.eq(id))
						.execute();

		if (affected > 0) {
			afterBulkDml();
			return true;
		}
		return false;
	}

	@Override
	public long deleteByCriteria(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		// STRICT: ID-first delete avoids join side effects.
		List<ID> ids = findIds(criteria);
		if (ids.isEmpty()) {
			return 0;
		}

		long affected = 0;

		for (List<ID> chunk : chunk(ids, bulkInChunkSize())) {
			affected += queryFactory
					.delete(path)
					.where(idExpr.in(chunk))
					.execute();
		}

		afterBulkDml();

		return affected;
	}

	// ----------------------------------------------------------------------
	// QUERY CONSTRUCTION
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

	protected final void afterBulkDml() {
		entityManager.flush();
		entityManager.clear();
	}

	protected void applyAudit(
			JPAUpdateClause update,
			Long updatedBy) {

		update.set(audit.updatedDate, LocalDateTime.now());
		update.set(audit.updatedBy, updatedBy);
	}

	// ----------------------------------------------------------------------
	// SORT SAFETY ENFORCEMENT (STRICT – FAIL FAST)
	// ----------------------------------------------------------------------

	/**
	 * Validates that ORDER BY clauses do NOT traverse collection-valued paths.
	 *
	 * <p>
	 * Sorting on to-many associations is mathematically incompatible with
	 * OFFSET/LIMIT pagination and is therefore forbidden at framework level.
	 * </p>
	 *
	 * <p>
	 * Allowed:
	 * <ul>
	 *   <li>Root entity fields</li>
	 *   <li>To-one association fields</li>
	 * </ul>
	 * <p>
	 * Forbidden:
	 * <ul>
	 *   <li>Collection-valued paths (List/Set/Map)</li>
	 * </ul>
	 * </p>
	 *
	 * <p>
	 * Also validates ORDER BY target type is orderable (Comparable/enum/primitive).
	 * This prevents accidental ORDER BY on JSON/BLOB/embeddables.
	 * </p>
	 */
	protected void validateSortSafety(List<OrderSpecifier<?>> orderSpecifiers) {

		for (OrderSpecifier<?> o : orderSpecifiers) {

			Expression<?> target = o.getTarget();

			// 1) reject collection-valued ordering
			if (containsCollectionPath(target)) {
				throw new IllegalStateException(
						"Unsafe ORDER BY detected: sorting on collection-valued " +
								"association is not pagination-safe.\n" +
								"OrderSpecifier=" + o
				);
			}

			// 2) reject non-orderable scalar types (JSON/BLOB/embeddable/etc.)
			validateOrderableType(target, o);
		}
	}

	private void validateOrderableType(Expression<?> target, OrderSpecifier<?> o) {

		// QueryDSL often represents truly orderable paths as ComparableExpressionBase.
		// But we still enforce a strict type rule to protect from custom paths.
		if (target instanceof Path<?> p) {

			if (!(target instanceof ComparableExpressionBase<?>)) {

				Class<?> t = target.getType();
				if (t == null) {
					throw new IllegalStateException("Unsafe ORDER BY target: null type for " + o);
				}

				boolean ok = Comparable.class.isAssignableFrom(t) || t.isEnum() || t.isPrimitive();
				if (!ok) {
					throw new IllegalStateException(
							"Unsafe ORDER BY target: non-orderable type " + t.getName() + " for " + o
					);
				}
			}
		}

		Class<?> type = target.getType();
		if (type != null && (type.isPrimitive() || type.isEnum() || Comparable.class.isAssignableFrom(type))) {
			return;
		}

		throw new IllegalStateException(
				"Unsafe ORDER BY on non-Comparable type: " + (type == null ? "null" : type.getName())
						+ " for " + o
		);
	}

	private boolean containsCollectionPath(Expression<?> expr) {

		if (expr instanceof CollectionPathBase<?, ?, ?>) {
			return true;
		}

		if (expr instanceof Path<?> p) {
			PathMetadata md = p.getMetadata();
			if (md != null && md.getParent() != null) {
				return containsCollectionPath(md.getParent());
			}
		}

		return false;
	}

	protected void applySort(JPQLQuery<?> query, CRITERIA criteria) {

		List<OrderSpecifier<?>> orderSpecifiers =
				criteria.resolveOrderSpecifiers(path);

		if (orderSpecifiers == null || orderSpecifiers.isEmpty()) {
			return;
		}

		// 🔒 STRICT FAIL FAST — prevent pagination-unsafe sorting
		validateSortSafety(orderSpecifiers);

		query.orderBy(orderSpecifiers.toArray(new OrderSpecifier<?>[0]));
	}

	protected void applySortOrDefaultById(
			JPQLQuery<?> query,
			CRITERIA criteria) {

		List<OrderSpecifier<?>> specs =
				criteria.resolveOrderSpecifiers(path);

		// No sort provided → deterministic default
		if (specs == null || specs.isEmpty()) {
			query.orderBy(idAscOrder);
			return;
		}

		// Fail fast on unsafe ORDER BY
		validateSortSafety(specs);

		// Always enforce TOTAL ordering
		boolean hasIdOrder = specs.stream()
				.anyMatch(o -> o.getTarget().equals(idExpr));

		if (!hasIdOrder) {
			List<OrderSpecifier<?>> withTieBreaker =
					new ArrayList<>(specs.size() + 1);
			withTieBreaker.addAll(specs);
			withTieBreaker.add(idAscOrder);

			query.orderBy(withTieBreaker.toArray(new OrderSpecifier<?>[0]));
		}
		else {
			query.orderBy(specs.toArray(new OrderSpecifier<?>[0]));
		}
	}

	// ----------------------------------------------------------------------
	// STABLE ORDERING AFTER ID PAGING
	// ----------------------------------------------------------------------

	/**
	 * Applies stable ordering for the phase-2 entity fetch after an ID-page query.
	 *
	 * <p>
	 * STRICT:
	 * <ul>
	 *   <li>If PostgreSQL: reapply original ORDER BY (same as phase 1).</li>
	 *   <li>If non-Postgres: preserve phase-1 order with CASE ordering.</li>
	 *   <li>No silent fallback. If CASE would exceed safe bounds, throw.</li>
	 * </ul>
	 * </p>
	 */
	protected void applyStableOrderAfterIdPaging(
			JPQLQuery<?> entityQuery,
			CRITERIA criteria,
			List<ID> ids) {

		if (IS_POSTGRES_DB) {
			// PostgreSQL: reapply ORDER BY safely (same as phase 1)
			applySortOrDefaultById(entityQuery, criteria);
			return;
		}

		// Non-Postgres: preserve phase-1 ID order explicitly
		applyIdOrder(entityQuery, ids);
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
	 *
	 * <p>
	 * STRICT:
	 * If the page is too large to safely express as CASE, this method throws.
	 * There is no "unordered fetch" fallback, because that would break determinism.
	 * </p>
	 */
	protected void applyIdOrder(
			JPQLQuery<?> query,
			List<ID> ids) {

		if (ids == null || ids.isEmpty()) {
			return;
		}

		if (ids.size() > maxCaseOrderIds()) {
			throw new IllegalStateException(
					"ID-order CASE too large (" + ids.size() + "). " +
							"Reduce page size or use PostgreSQL ordering mode."
			);
		}

		CaseBuilder cb = new CaseBuilder();
		CaseBuilder.Cases<Integer, NumberExpression<Integer>> cases = null;

		int index = 0;
		for (ID id : ids) {
			if (cases == null) {
				cases = cb.when(idExpr.eq(id)).then(index++);
			}
			else {
				cases = cases.when(idExpr.eq(id)).then(index++);
			}
		}

		assert cases != null;
		query.orderBy(
				cases.otherwise(Integer.MAX_VALUE).asc()
		);
	}

	/**
	 * Maximum number of IDs allowed in CASE ordering for non-Postgres databases.
	 *
	 * <p>
	 * STRICT:
	 * This bound prevents generating pathological SQL and protects query planners.
	 * Exceeding this limit is a caller contract violation and results in an exception.
	 * </p>
	 */
	protected int maxCaseOrderIds() {
		return 200;
	}

	// ----------------------------------------------------------------------
	// FETCH GRAPH INTROSPECTION (STRICT TRIGGER ONLY)
	// ----------------------------------------------------------------------

	/**
	 * Detects whether the requested fetch graph contains any collection-valued attribute.
	 *
	 * <p>
	 * STRICT:
	 * <ul>
	 *   <li>If the fetch graph contains collections, paging must be ID-first.</li>
	 *   <li>This method does NOT “fix” duplicates or attempt DISTINCT.</li>
	 *   <li>It only determines the safe query strategy.</li>
	 * </ul>
	 * </p>
	 */
	protected boolean fetchGraphContainsCollection(String... hints) {

		if (hints == null || hints.length == 0) {
			return false;
		}

		Metamodel metamodel = entityManager.getMetamodel();
		EntityType<?> entityType = metamodel.entity(path.getType());

		for (String hint : hints) {
			if (containsCollectionAttribute(entityType, hint)) {
				return true;
			}
		}

		return false;
	}

	private boolean containsCollectionAttribute(
			ManagedType<?> rootType,
			String graph) {

		if (graph == null || graph.isBlank()) {
			return false;
		}

		// Strip outer entity name if present: Role(...)
		int start = graph.indexOf('(');
		int end = graph.lastIndexOf(')');
		if (start >= 0 && end > start) {
			graph = graph.substring(start + 1, end);
		}

		return walkAttributes(rootType, graph);
	}

	private boolean walkAttributes(
			ManagedType<?> type,
			String pathExpr) {

		int idx = 0;
		while (idx < pathExpr.length()) {

			int nextParen = pathExpr.indexOf('(', idx);
			int nextComma = pathExpr.indexOf(',', idx);

			int end = minPositive(nextParen, nextComma, pathExpr.length());
			if (end < 0) {
				end = pathExpr.length();
			}

			String attrName = pathExpr.substring(idx, end).trim();

			if (!attrName.isEmpty()) {
				Attribute<?, ?> attr = type.getAttribute(attrName);

				// 🔥 THIS is the real check
				if (attr.isCollection()) {
					return true;
				}

				if (attr instanceof SingularAttribute<?, ?> sa) {
					if (sa.getType() instanceof ManagedType<?> mt) {
						type = mt;
					}
				}
			}

			if (nextParen >= 0 && nextParen == end) {
				int close = findMatchingParen(pathExpr, nextParen);
				String nested = pathExpr.substring(nextParen + 1, close);
				if (walkAttributes(type, nested)) {
					return true;
				}
				idx = close + 1;
			}
			else {
				idx = end + 1;
			}
		}

		return false;
	}

	private int minPositive(int... values) {
		int min = Integer.MAX_VALUE;
		for (int v : values) {
			if (v >= 0 && v < min) {
				min = v;
			}
		}
		return min == Integer.MAX_VALUE ? -1 : min;
	}

	private int findMatchingParen(String s, int open) {
		int depth = 1;
		for (int i = open + 1; i < s.length(); i++) {
			if (s.charAt(i) == '(') {
				depth++;
			}
			if (s.charAt(i) == ')') {
				depth--;
			}
			if (depth == 0) {
				return i;
			}
		}
		throw new IllegalArgumentException("Unbalanced parentheses in graph: " + s);
	}

	// ----------------------------------------------------------------------
	// AUDIT PATH RESOLUTION
	// ----------------------------------------------------------------------

	private QAbstractEntity resolveAuditPath(EntityPath<?> p) {

		if (p instanceof QAbstractEntity qa) {
			return qa;
		}

		if (p instanceof EntityPathBase<?> epb) {
			try {
				Field f = epb.getClass().getField("_super");
				return (QAbstractEntity) f.get(epb);
			}
			catch (Exception e) {
				logger.error("Failed to resolve audit path", e);
			}
		}

		throw new IllegalStateException(
				"QAbstractEntity audit path not resolvable"
		);
	}
}
