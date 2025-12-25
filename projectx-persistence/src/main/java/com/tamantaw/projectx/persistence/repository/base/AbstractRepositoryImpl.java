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

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Base JPA repository implementation with:
 *
 * <ul>
 *   <li>QueryDSL-based dynamic criteria</li>
 *   <li>N+1-safe fetching via fetch graphs</li>
 *   <li>Pagination-safe ordering</li>
 *   <li>Deterministic, database-independent semantics</li>
 *   <li>Explicit API contracts (no hidden magic)</li>
 * </ul>
 *
 * <p>
 * <b>Important ordering contract</b>
 * <ul>
 *   <li>
 *     Joined ordering (to-one paths only) is applied ONLY in the ID-selection phase.
 *   </li>
 *   <li>
 *     Entity fetch phase is ordered strictly by primary key (ID ASC).
 *   </li>
 *   <li>
 *     This avoids DB-specific behavior, CASE explosions, and silent corruption.
 *   </li>
 * </ul>
 *
 * <p>
 * This repository is designed for correctness and transactional domain access.
 * It is NOT intended for large-scale reporting or bulk exports.
 * </p>
 */
public abstract class AbstractRepositoryImpl<
		ENTITY extends AbstractEntity,
		QCLAZZ extends EntityPathBase<ENTITY>,
		CRITERIA extends AbstractCriteria<QCLAZZ>,
		ID extends Long>
		extends SimpleJpaRepository<ENTITY, ID>
		implements AbstractRepository<ENTITY, QCLAZZ, CRITERIA, ID> {

	// ----------------------------------------------------------------------
	// CORE FIELDS
	// ----------------------------------------------------------------------

	private static final EntityPathResolver PATH_RESOLVER =
			SimpleEntityPathResolver.INSTANCE;

	protected final Logger logger =
			LogManager.getLogger("repositoryLogs." + getClass().getSimpleName());

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

		@SuppressWarnings("unchecked")
		QCLAZZ resolved = (QCLAZZ) PATH_RESOLVER.createPath(domainClass);
		path = resolved;

		PathBuilder<ENTITY> builder =
				new PathBuilder<>(path.getType(), path.getMetadata());

		querydsl = new Querydsl(entityManager, builder);
		queryFactory = new JPAQueryFactory(entityManager);

		audit = resolveAuditPath(path);
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

		boolean requiresIdFirst =
				fetchGraphContainsCollection(hints)
						|| criteria.hasJoinedSort();

		// ------------------------------------------------------------
		// FAST PATH — no collection fetch, no joined sort
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
		// SAFE PATH — ID FIRST
		// ------------------------------------------------------------
		JPQLQuery<Long> idQuery =
				createQuery(filter).select(audit.id);

		applySortOrDefaultById(idQuery, criteria);

		List<Long> ids = idQuery.limit(2).fetch();

		if (ids.isEmpty()) {
			return Optional.empty();
		}
		if (ids.size() > 1) {
			throw new IllegalStateException(
					"findOne() returned more than one result"
			);
		}

		JPQLQuery<ENTITY> entityQuery =
				createQuery(audit.id.eq(ids.getFirst()), hints)
						.select(path);

		// Identity is unique for a single ID
		return Optional.ofNullable(entityQuery.fetchOne());
	}

	// ----------------------------------------------------------------------
	// COUNT / EXISTS / IDS
	// ----------------------------------------------------------------------

	/**
	 * Returns all matching entities (no pagination).
	 *
	 * <p>
	 * Paging is intentionally forbidden here to prevent unsafe JOIN + OFFSET usage.
	 * </p>
	 */
	@Override
	public List<ENTITY> findAll(CRITERIA criteria, String... hints) {

		Assert.notNull(criteria, "Criteria must not be null");

		if (criteria.toPageable() != null) {
			throw new IllegalStateException(
					"Paging is not supported in findAll(). Use findByPaging()."
			);
		}

		Predicate filter = criteria.getFilter(path);

		boolean requiresIdFirst =
				fetchGraphContainsCollection(hints)
						|| criteria.hasJoinedSort();

		// ------------------------------------------------------------
		// FAST PATH — safe to use DISTINCT
		// ------------------------------------------------------------
		if (!requiresIdFirst) {

			JPQLQuery<ENTITY> query =
					createQuery(filter, hints).select(path);

			applySortOrDefaultById(query, criteria);
			query.distinct();

			return query.fetch();
		}

		// ------------------------------------------------------------
		// SAFE PATH — ID FIRST (NO DISTINCT!)
		// ------------------------------------------------------------
		JPQLQuery<Long> idQuery =
				createQuery(filter).select(audit.id);

		applySortOrDefaultById(idQuery, criteria);

		List<Long> ids = idQuery.fetch();
		if (ids.isEmpty()) {
			return List.of();
		}

		JPQLQuery<ENTITY> entityQuery =
				createQuery(audit.id.in(ids), hints).select(path);

		applyIdOrder(entityQuery, ids);

		List<ENTITY> rows = entityQuery.fetch();

		// ✅ Deduplicate safely in memory
		Map<Long, ENTITY> unique = new LinkedHashMap<>();
		for (ENTITY e : rows) {
			unique.putIfAbsent(e.getId(), e);
		}

		return new ArrayList<>(unique.values());
	}

	/**
	 * Pagination-safe read.
	 */
	@Override
	public Page<ENTITY> findByPaging(CRITERIA criteria, String... hints) {

		Assert.notNull(criteria, "Criteria must not be null");

		Pageable pageable = criteria.toPageable();
		Assert.notNull(pageable, "Pageable must not be null");

		Predicate filter = criteria.getFilter(path);

		boolean requiresIdFirst =
				fetchGraphContainsCollection(hints)
						|| criteria.hasJoinedSort();

		// ------------------------------------------------------------
		// FAST PATH — DISTINCT allowed
		// ------------------------------------------------------------
		if (!requiresIdFirst) {

			JPQLQuery<ENTITY> query =
					createQuery(filter, hints).select(path);

			applySortOrDefaultById(query, criteria);

			query = query
					.offset(pageable.getOffset())
					.limit(pageable.getPageSize());

			List<ENTITY> content = query.fetch();
			long total = count(criteria);

			return new PageImpl<>(content, pageable, total);
		}

		// ------------------------------------------------------------
		// PHASE 1 — ID PAGE
		// ------------------------------------------------------------
		JPQLQuery<Long> idQuery =
				createQuery(filter).select(audit.id);

		applySortOrDefaultById(idQuery, criteria);

		idQuery = idQuery
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize());

		List<Long> ids = idQuery.fetch();
		if (ids.isEmpty()) {
			return Page.empty(pageable);
		}

		// ------------------------------------------------------------
		// PHASE 2 — ENTITY FETCH (NO DISTINCT!)
		// ------------------------------------------------------------
		JPQLQuery<ENTITY> entityQuery =
				createQuery(audit.id.in(ids), hints).select(path);

		applyIdOrder(entityQuery, ids);

		List<ENTITY> rows = entityQuery.fetch();

		// Deduplicate while preserving order
		Map<Long, ENTITY> unique = new LinkedHashMap<>();
		for (ENTITY e : rows) {
			unique.putIfAbsent(e.getId(), e);
		}

		long total = count(criteria);

		return new PageImpl<>(
				new ArrayList<>(unique.values()),
				pageable,
				total
		);
	}

	@Override
	public long count(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		Long count = createQuery(filter)
				.select(audit.id.countDistinct())
				.fetchOne();

		return count == null ? 0L : count;
	}

	// ----------------------------------------------------------------------
	// BULK WRITE OPERATIONS
	// ----------------------------------------------------------------------

	@Override
	public boolean exists(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		return createQuery(filter)
				.select(audit.id)
				.fetchFirst() != null;
	}

	@Override
	public List<Long> findIds(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		JPQLQuery<Long> query =
				createQuery(filter).select(audit.id);

		applySort(query, criteria);
		return query.fetch();
	}

	@Override
	public ENTITY saveRecord(ENTITY entity) {
		return super.saveAndFlush(entity);
	}

	@Override
	public List<ENTITY> saveAllRecords(Iterable<ENTITY> entities) {
		return super.saveAllAndFlush(entities);
	}

	@Override
	public long updateById(UpdateSpec<ENTITY> spec, long id, long updatedBy) {

		Assert.notNull(spec, "UpdateSpec must not be null");

		JPAUpdateClause update = queryFactory.update(path);
		applyAudit(update, updatedBy);
		spec.apply(update, path);

		long affected = update.where(audit.id.eq(id)).execute();
		afterBulkDml();

		return affected;
	}

	@Override
	public <E extends ENTITY> long updateByCriteria(
			UpdateSpec<E> spec,
			CRITERIA criteria,
			Long updatedBy) {

		Assert.notNull(criteria, "Criteria must not be null");

		@SuppressWarnings("unchecked")
		EntityPathBase<E> typedPath = (EntityPathBase<E>) path;

		Predicate filter = criteria.getFilter(path);

		JPAUpdateClause update = queryFactory.update(typedPath);
		applyAudit(update, updatedBy);
		spec.apply(update, typedPath);

		long affected = update.where(filter).execute();
		afterBulkDml();
		return affected;
	}

	@Override
	public boolean deleteWithId(@Nonnull ID id) {

		Assert.notNull(id, "Id must not be null");

		long affected = queryFactory
				.delete(path)
				.where(audit.id.eq(id))
				.execute();

		if (affected > 0) {
			afterBulkDml();
		}
		return affected > 0;
	}

	@Override
	public long deleteByCriteria(CRITERIA criteria) {

		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		long affected = queryFactory.delete(path)
				.where(filter)
				.execute();

		afterBulkDml();
		return affected;
	}

	// ----------------------------------------------------------------------
	// SORT SAFETY
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

	protected void applySort(JPQLQuery<?> query, CRITERIA criteria) {

		List<OrderSpecifier<?>> specs =
				criteria.resolveOrderSpecifiers(path);

		if (specs == null || specs.isEmpty()) {
			return;
		}

		validateSortSafety(specs);
		query.orderBy(specs.toArray(new OrderSpecifier<?>[0]));
	}

	protected void applySortOrDefaultById(
			JPQLQuery<?> query,
			CRITERIA criteria) {

		List<OrderSpecifier<?>> specs =
				criteria.resolveOrderSpecifiers(path);

		if (specs == null || specs.isEmpty()) {
			query.orderBy(audit.id.asc());
			return;
		}

		validateSortSafety(specs);
		query.orderBy(specs.toArray(new OrderSpecifier<?>[0]));
	}

	protected void validateSortSafety(List<OrderSpecifier<?>> orderSpecifiers) {
		for (OrderSpecifier<?> o : orderSpecifiers) {

			Expression<?> target = o.getTarget();

			// 1) reject collection-valued ordering
			if (containsCollectionPath(target)) {
				throw new IllegalStateException(
						"Unsafe ORDER BY on collection-valued path: " + o
				);
			}

			// 2) reject non-orderable scalar types (JSON/BLOB/embeddable/etc.)
			validateOrderableType(target, o);
		}
	}

	private void validateOrderableType(Expression<?> target, OrderSpecifier<?> o) {

		if (target instanceof Path<?> p) {
			// If it is a QueryDSL path but not a ComparableExpression,
			// it's likely not meant to be sorted.
			if (!(target instanceof ComparableExpressionBase<?>)) {

				Class<?> t = target.getType();
				if (t == null || !(Comparable.class.isAssignableFrom(t) || t.isEnum() || t.isPrimitive())) {
					throw new IllegalStateException("Unsafe ORDER BY target: " + o);
				}
			}
		}

		// fallback rule (same as minimal)
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

	// ----------------------------------------------------------------------
	// READ OPERATIONS
	// ----------------------------------------------------------------------

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

	protected void applyIdOrder(
			JPQLQuery<?> query,
			List<Long> ids) {

		if (ids == null || ids.isEmpty()) {
			return;
		}

		if (ids.size() > maxCaseOrderIds()) {
			logger.warn("ID-order CASE too large ({}). Falling back to unordered fetch + in-memory reorder.", ids.size());
			// fallback: no ORDER BY in SQL
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

		assert cases != null;
		query.orderBy(
				cases.otherwise(Integer.MAX_VALUE).asc()
		);
	}

	protected void reorderInMemory(List<ENTITY> content, List<Long> ids) {

		if (content.size() <= 1) {
			return;
		}

		Map<Long, Integer> position = new HashMap<>(ids.size() * 2);
		for (int i = 0; i < ids.size(); i++) {
			position.put(ids.get(i), i);
		}

		content.sort(Comparator.comparingInt(
				e -> {
					Long id = e.getId();
					return id != null ? position.getOrDefault(id, Integer.MAX_VALUE) : Integer.MAX_VALUE;
				}
		));
	}

	protected int maxCaseOrderIds() {
		return 200;
	}

	protected void applyAudit(
			JPAUpdateClause update,
			Long updatedBy) {

		update.set(audit.updatedDate, LocalDateTime.now());
		update.set(audit.updatedBy, updatedBy);
	}
}
