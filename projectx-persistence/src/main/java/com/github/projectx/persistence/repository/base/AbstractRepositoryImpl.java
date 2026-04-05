package com.github.projectx.persistence.repository.base;

import com.github.projectx.persistence.config.RepositoryRuntimeProperties;
import com.github.projectx.persistence.criteria.base.AbstractCriteria;
import com.github.projectx.persistence.entity.base.AbstractEntity;
import com.github.projectx.persistence.entity.base.QAbstractEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.JoinExpression;
import com.querydsl.core.types.*;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.AbstractJPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Subgraph;
import jakarta.persistence.metamodel.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.support.MutableQueryHints;
import org.springframework.data.jpa.repository.support.QueryHints;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.util.Assert;

import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;

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
		ID extends Serializable & Comparable<ID>,
		ENTITY extends AbstractEntity,
		QCLAZZ extends EntityPathBase<ENTITY>,
		CRITERIA extends AbstractCriteria<QCLAZZ>>
		implements AbstractRepository<ID, ENTITY, QCLAZZ, CRITERIA> {

	// ----------------------------------------------------------------------
	// STATIC CONFIGURATION
	// ----------------------------------------------------------------------

	private static final String POSTGRES_MODE_AUTO = "auto";
	private static final String POSTGRES_MODE_TRUE = "true";
	private static final String POSTGRES_MODE_FALSE = "false";

	private static final EntityPathResolver PATH_RESOLVER =
			SimpleEntityPathResolver.INSTANCE;

	private static final Logger logger =
			LogManager.getLogger("repositoryLogs." + AbstractRepositoryImpl.class.getName());

	/**
	 * Conservative defaults used when no property overrides are supplied.
	 * These values are intentionally bounded to keep SQL generated for
	 * IN-chunk processing and CASE ordering within predictable limits.
	 */
	private static final int DEFAULT_BULK_IN_CHUNK_SIZE = RepositoryRuntimeProperties.DEFAULT_BULK_IN_CHUNK_SIZE;
	private static final int DEFAULT_FETCH_IN_CHUNK_SIZE = RepositoryRuntimeProperties.DEFAULT_FETCH_IN_CHUNK_SIZE;
	private static final int DEFAULT_MAX_CASE_ORDER_IDS = RepositoryRuntimeProperties.DEFAULT_MAX_CASE_ORDER_IDS;
	private static final int DEFAULT_GRAPH_CACHE_MAX_SIZE = RepositoryRuntimeProperties.DEFAULT_GRAPH_CACHE_MAX_SIZE;
	private static final int DEFAULT_KEYSET_LIMIT = 20;

	/**
	 * Guard rails for externally configured repository limits.
	 * Invalid values are rejected and replaced with defaults during initialization.
	 */
	private static final int MIN_CHUNK_SIZE = 1;
	private static final int MAX_CHUNK_SIZE = 10_000;
	private static final int MIN_CASE_ORDER_IDS = 1;
	private static final int MAX_CASE_ORDER_IDS = 10_000;
	private static final int MIN_GRAPH_CACHE_MAX_SIZE = 100;
	private static final int MAX_GRAPH_CACHE_MAX_SIZE = 50_000;

	// ----------------------------------------------------------------------
	// CORE FIELDS
	// ----------------------------------------------------------------------

	protected final Class<ENTITY> domainClass;
	protected final Class<ID> idClass;
	protected final SimpleExpression<ID> idExpr;
	/**
	 * Comparable ID path used by keyset cursor predicate ({@code id > cursor}).
	 */
	protected final ComparableExpression<ID> idComparableExpr;
	protected final OrderSpecifier<?> idAscOrder;
	protected final QCLAZZ path;
	protected final QAbstractEntity audit;
	protected final String rootEntitySimpleName;
	/**
	 * Caches for fetch-graph parsing and collection detection.
	 * These avoid repeated metamodel traversal for hot endpoints using the same graph hints.
	 */
	private final Map<String, String> normalizedGraphSpecCache = new ConcurrentHashMap<>();
	private final Map<String, List<String>> topLevelGraphTokenCache = new ConcurrentHashMap<>();
	private final Map<String, Boolean> collectionFetchGraphCache = new ConcurrentHashMap<>();
	protected EntityManager entityManager;
	protected final JPAQueryFactory queryFactory =
			new JPAQueryFactory(() -> entityManager);
	/**
	 * Typed runtime overrides for query chunk/ordering bounds.
	 * Repositories default safely even if property binding is unavailable during isolated construction.
	 */
	private RepositoryRuntimeProperties repositoryRuntimeProperties = new RepositoryRuntimeProperties();
	private int effectiveBulkInChunkSize = DEFAULT_BULK_IN_CHUNK_SIZE;
	private int effectiveFetchInChunkSize = DEFAULT_FETCH_IN_CHUNK_SIZE;
	private int effectiveMaxCaseOrderIds = DEFAULT_MAX_CASE_ORDER_IDS;
	private int effectiveGraphCacheMaxSize = DEFAULT_GRAPH_CACHE_MAX_SIZE;
	private boolean effectivePostgresDb;
	private boolean initialized;

	// ----------------------------------------------------------------------
	// CONSTRUCTOR
	// ----------------------------------------------------------------------

	protected AbstractRepositoryImpl(
			Class<ENTITY> domainClass,
			Class<ID> idClass) {

		Class<ENTITY> safeDomainClass = Objects.requireNonNull(domainClass, "Domain class must not be null");
		Class<ID> safeIdClass = Objects.requireNonNull(idClass, "Id class must not be null");
		this.domainClass = safeDomainClass;
		this.idClass = safeIdClass;

		@SuppressWarnings("unchecked")
		QCLAZZ resolved = (QCLAZZ) Objects.requireNonNull(
				PATH_RESOLVER.createPath(safeDomainClass),
				"Entity path must not be null"
		);
		path = resolved;
		audit = Objects.requireNonNull(resolveAuditPath(resolved), "Audit path must not be null");
		rootEntitySimpleName = Objects.requireNonNull(
				safeDomainClass.getSimpleName(),
				"Root entity simple name must not be null"
		);
		idExpr = Objects.requireNonNull(
				Expressions.simplePath(safeIdClass, resolved, "id"),
				"ID expression must not be null"
		);
		idComparableExpr = Objects.requireNonNull(
				Expressions.comparablePath(safeIdClass, resolved, "id"),
				"Comparable ID expression must not be null"
		);
		idAscOrder = new OrderSpecifier<>(Order.ASC, (Expression<? extends Comparable<?>>) idExpr);
	}

	private static <T> List<List<T>> chunk(List<T> src, int size) {

		// Chunking keeps IN-clause cardinality bounded to protect DB planners,
		// while still preserving deterministic full result assembly in Java.
		if (src.isEmpty()) {
			return List.of();
		}
		if (size <= 0) {
			throw new IllegalStateException("Chunk size must be > 0 but was " + size);
		}

		List<List<T>> out =
				new ArrayList<>((src.size() + size - 1) / size);

		for (int i = 0; i < src.size(); i += size) {
			out.add(src.subList(i, Math.min(src.size(), i + size)));
		}

		return out;
	}

	static <T> T withSinglePhase2Retry(
			Logger log,
			String operation,
			Supplier<T> attemptSupplier
	) {
		Objects.requireNonNull(log, "log must not be null");
		Objects.requireNonNull(operation, "operation must not be null");
		Objects.requireNonNull(attemptSupplier, "attemptSupplier must not be null");
		try {
			return attemptSupplier.get();
		}
		catch (Phase2EntityMissException firstFailure) {
			log.warn(
					"{} phase-2 fetch missed selected root ids={} on first attempt. Replaying read once.",
					operation,
					firstFailure.missingIds()
			);
			return attemptSupplier.get();
		}
	}

	// ----------------------------------------------------------------------
	// READ OPERATIONS
	// ----------------------------------------------------------------------

	@Autowired
	void setRepositoryRuntimeProperties(RepositoryRuntimeProperties repositoryRuntimeProperties) {
		this.repositoryRuntimeProperties =
				Objects.requireNonNull(repositoryRuntimeProperties, "repositoryRuntimeProperties must not be null");
	}

	protected void initialize(EntityManager entityManager) {

		Objects.requireNonNull(entityManager, "EntityManager must not be null");
		Assert.state(!initialized, "AbstractRepositoryImpl is already initialized");

		this.entityManager = entityManager;
		effectivePostgresDb = resolvePostgresDbMode();

		IdentifiableType<ENTITY> identifiable =
				(IdentifiableType<ENTITY>) entityManager
						.getMetamodel()
						.managedType(domainClass);

		SingularAttribute<? super ENTITY, ID> idAttr =
				identifiable.getId(idClass);

		if (!"id".equals(idAttr.getName())) {
			throw new IllegalStateException(
					"Only id attribute named 'id' is supported for deterministic paging. Found: " + idAttr.getName()
			);
		}

		if (!Comparable.class.isAssignableFrom(idClass)) {
			throw new IllegalStateException(
					"ID type must be Comparable for deterministic paging. idClass=" + idClass.getName()
			);
		}

		applyConfiguredLimits();
		initialized = true;
	}

	private boolean resolvePostgresDbMode() {
		String configuredMode = repositoryRuntimeProperties.getPostgresDb();
		String mode = Objects.toString(configuredMode, POSTGRES_MODE_AUTO)
				.trim()
				.toLowerCase(Locale.ROOT);

		return switch (mode) {
			case POSTGRES_MODE_TRUE -> true;
			case POSTGRES_MODE_FALSE -> false;
			case POSTGRES_MODE_AUTO -> detectPostgresDatabase();
			default -> {
				logger.warn(
						"Invalid value for projectx.repository.postgres-db='{}'. " +
								"Using auto-detection for repository {}.",
						configuredMode,
						domainClass.getSimpleName()
				);
				yield detectPostgresDatabase();
			}
		};
	}

	private boolean detectPostgresDatabase() {
		Map<String, Object> factoryProps = entityManager.getEntityManagerFactory().getProperties();
		if (containsPostgres(factoryProps.get("hibernate.dialect"))
				|| containsPostgres(factoryProps.get("jakarta.persistence.jdbc.url"))
				|| containsPostgres(factoryProps.get("spring.jpa.database-platform"))
				|| containsPostgres(factoryProps.get("spring.datasource.url"))) {
			return true;
		}

		try {
			Session session = entityManager.unwrap(Session.class);
			Boolean detected = session.doReturningWork(connection -> {
				String databaseProductName = connection.getMetaData().getDatabaseProductName();
				return containsPostgres(databaseProductName);
			});
			return Boolean.TRUE.equals(detected);
		}
		catch (RuntimeException e) {
			logger.warn(
					"Could not auto-detect database vendor for repository {}. " +
							"Falling back to non-PostgreSQL ordering strategy.",
					domainClass.getSimpleName(),
					e
			);
			return false;
		}
	}

	private boolean containsPostgres(@Nullable Object value) {
		return value != null && value.toString()
				.toLowerCase(Locale.ROOT)
				.contains("postgres");
	}

	protected final void assertInitialized() {
		Assert.state(initialized, "AbstractRepositoryImpl has not been initialized");
		Assert.state(entityManager != null, "EntityManager must be initialized");
	}

	private ManagedType<?> rootManagedType() {
		return Objects.requireNonNull(
				entityManager.getMetamodel().managedType(domainClass),
				"Root managed type must not be null"
		);
	}

	/**
	 * Applies externalized repository bounds once at initialization time.
	 *
	 * <p>
	 * Centralizing this keeps all pagination/bulk paths consistent and makes
	 * operational tuning explicit via properties without changing code.
	 * </p>
	 */
	private void applyConfiguredLimits() {

		effectiveBulkInChunkSize = sanitizeConfiguredInt(
				"projectx.repository.bulk-in-chunk-size",
				repositoryRuntimeProperties.getBulkInChunkSize(),
				DEFAULT_BULK_IN_CHUNK_SIZE,
				MIN_CHUNK_SIZE,
				MAX_CHUNK_SIZE
		);

		effectiveFetchInChunkSize = sanitizeConfiguredInt(
				"projectx.repository.fetch-in-chunk-size",
				repositoryRuntimeProperties.getFetchInChunkSize(),
				DEFAULT_FETCH_IN_CHUNK_SIZE,
				MIN_CHUNK_SIZE,
				MAX_CHUNK_SIZE
		);

		effectiveMaxCaseOrderIds = sanitizeConfiguredInt(
				"projectx.repository.max-case-order-ids",
				repositoryRuntimeProperties.getMaxCaseOrderIds(),
				DEFAULT_MAX_CASE_ORDER_IDS,
				MIN_CASE_ORDER_IDS,
				MAX_CASE_ORDER_IDS
		);

		effectiveGraphCacheMaxSize = sanitizeConfiguredInt(
				"projectx.repository.graph-cache-max-size",
				repositoryRuntimeProperties.getGraphCacheMaxSize(),
				DEFAULT_GRAPH_CACHE_MAX_SIZE,
				MIN_GRAPH_CACHE_MAX_SIZE,
				MAX_GRAPH_CACHE_MAX_SIZE
		);
	}

	private int sanitizeConfiguredInt(
			String key,
			int configuredValue,
			int defaultValue,
			int min,
			int max) {

		// Fail closed: invalid runtime configuration should not leak into query construction.
		if (configuredValue >= min && configuredValue <= max) {
			return configuredValue;
		}

		logger.warn(
				"Invalid repository property {}={} (allowed range {}..{}). Falling back to {}.",
				key,
				configuredValue,
				min,
				max,
				defaultValue
		);
		return defaultValue;
	}

	protected @Nullable QueryHints getRelatedDataHints(String @Nullable ... hints) {

		if (hints == null || hints.length == 0) {
			return null;
		}

		// Build a dynamic, validated fetch graph so callers can request relations
		// without allowing arbitrary/unsafe path strings.
		Class<?> rootType = Objects.requireNonNull(path.getType(), "Root entity type must not be null");
		EntityGraph<?> graph = entityManager.createEntityGraph(rootType);
		ManagedType<?> managedType = rootManagedType();

		for (String hint : hints) {
			String normalizedHint = Objects.toString(hint, "");
			if (normalizedHint.isBlank()) {
				continue;
			}
			String graphSpec = normalizeGraphSpec(normalizedHint, rootEntitySimpleName);
			applyGraphSpec(new EntityGraphContainer(graph), managedType, graphSpec);
		}

		MutableQueryHints qh = new MutableQueryHints();
		// Support both hint namespaces for compatibility across JPA ProductProvider versions.
		qh.add("jakarta.persistence.fetchgraph", graph);
		return qh;
	}

	/**
	 * Strictly deduplicates root entities by ID while preserving order.
	 */
	protected Map<ID, ENTITY> deduplicateById(List<ENTITY> rows) {

		if (rows.isEmpty()) {
			return Map.of();
		}

		Map<ID, ENTITY> unique = new LinkedHashMap<>(rows.size());

		for (ENTITY e : rows) {
			@SuppressWarnings("unchecked")
			ID id = (ID) e.getId();
			unique.putIfAbsent(id, e);
		}

		return unique;
	}

	private List<ENTITY> materializeOrderedEntities(
			List<ID> ids,
			Map<ID, ENTITY> uniqueRows,
			String operation
	) {
		if (ids.isEmpty()) {
			return List.of();
		}

		List<ENTITY> ordered = new ArrayList<>(ids.size());
		List<ID> missingIds = null;
		for (ID id : ids) {
			ENTITY entity = uniqueRows.get(id);
			if (entity == null) {
				if (missingIds == null) {
					missingIds = new ArrayList<>();
				}
				missingIds.add(id);
				continue;
			}
			ordered.add(entity);
		}
		if (missingIds != null && !missingIds.isEmpty()) {
			throw new Phase2EntityMissException(operation, missingIds);
		}
		return ordered;
	}

	protected String normalizeGraphSpec(@Nullable String graph, String rootName) {

		if (graph == null || graph.isBlank()) {
			return "";
		}

		String input = graph.trim();
		String cacheKey = rootName + "|" + input;

		return computeCached(
				normalizedGraphSpecCache,
				cacheKey,
				ignored -> normalizeGraphSpecUncached(input, rootName)
		);
	}

	private <K, V> V computeCached(
			Map<K, V> cache,
			K key,
			Function<? super K, ? extends V> computer) {

		V cached = cache.get(key);
		if (cached != null) {
			return cached;
		}

		int cacheLimit = effectiveGraphCacheMaxSize;
		if (cacheLimit > 0 && cache.size() >= cacheLimit) {
			cache.clear();
		}

		return cache.computeIfAbsent(key, computer);
	}

	/**
	 * Normalizes both accepted graph formats:
	 * <ul>
	 *   <li>{@code Root(attr1,attr2)}</li>
	 *   <li>{@code attr1,attr2}</li>
	 * </ul>
	 * into a single inner-spec representation.
	 */
	private String normalizeGraphSpecUncached(String graph, String rootName) {

		// Must be either:
		//  - Root(...)
		//  - attr1,attr2(...)
		if (graph.startsWith(rootName + "(")) {
			int end = graph.lastIndexOf(')');
			if (end < 0) {
				throw new IllegalArgumentException("Unbalanced parentheses in graph: " + graph);
			}
			return graph.substring(rootName.length() + 1, end).trim();
		}

		// Root alone = no-op
		if (graph.equals(rootName)) {
			return "";
		}

		// No root prefix → assume already inner spec
		return graph;
	}

	protected void applyGraphSpec(
			GraphContainer graph,
			ManagedType<?> rootType,
			String graphSpec) {

		if (graphSpec.isBlank()) {
			return;
		}

		for (String token : splitTopLevel(graphSpec)) {

			if (token.isEmpty()) {
				continue;
			}

			int paren = token.indexOf('(');

			// --------------------------------------------------
			// SIMPLE ATTRIBUTE (root-level only)
			// --------------------------------------------------
			if (paren < 0) {
				validateAttribute(rootType, token);
				graph.addAttribute(token);
				continue;
			}

			// --------------------------------------------------
			// NESTED ATTRIBUTE
			// --------------------------------------------------
			String attrName = token.substring(0, paren).trim();
			String nestedSpec = token.substring(paren + 1, token.length() - 1).trim();

			Attribute<?, ?> attr = validateAttribute(rootType, attrName);
			ManagedType<?> nestedType = resolveManagedType(attr);

			Subgraph<?> subgraph =
					graph.addSubgraph(attr, nestedType);

			// 🔒 IMPORTANT:
			// Nested parsing is scoped ONLY to nestedType
			applyGraphSpec(
					new SubgraphContainer(subgraph),
					nestedType,
					nestedSpec
			);
		}
	}

	private Attribute<?, ?> validateAttribute(
			ManagedType<?> type,
			String attrName) {

		try {
			return type.getAttribute(attrName);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalStateException(
					"Invalid fetch graph attribute '" + attrName +
							"' for entity " + type.getJavaType().getSimpleName(),
					e
			);
		}
	}

	protected ManagedType<?> resolveManagedType(Attribute<?, ?> attr) {

		if (attr instanceof SingularAttribute<?, ?> sa) {
			if (sa.getType() instanceof ManagedType<?> mt) {
				return mt;
			}
		}

		if (attr instanceof PluralAttribute<?, ?, ?> pa) {
			if (pa.getElementType() instanceof ManagedType<?> mt) {
				return mt;
			}
		}

		throw new IllegalStateException(
				"Attribute '" + attr.getName() + "' is not an entity path and cannot have nested graph hints"
		);
	}

	/**
	 * Simple primary-key lookup.
	 */
	@Override
	public Optional<ENTITY> findById(ID id) {
		assertInitialized();
		Assert.notNull(id, "Id must not be null");
		return Optional.ofNullable(entityManager.find(domainClass, id));
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
	public Optional<ENTITY> findOne(CRITERIA criteria, String @Nullable ... hints) {

		assertInitialized();
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
		assertNoAdditionalJoins((AbstractJPAQuery<?, ?>) idQuery, "findOne.idQuery");

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

		List<ENTITY> rows = entityQuery.fetch();
		Map<ID, ENTITY> unique = deduplicateById(rows);

		if (unique.isEmpty()) {
			return Optional.empty();
		}

		if (unique.size() > 1) {
			throw new IllegalStateException(
					"findOne() returned more than one unique root for a single ID"
			);
		}

		return Optional.of(unique.values().iterator().next());
	}

	/**
	 * Returns all matching entities without pagination.
	 *
	 * <p>
	 * Paging is NOT allowed here to prevent unsafe joins + pagination bugs.
	 * </p>
	 */
	@Override
	public List<ENTITY> findAll(CRITERIA criteria, String @Nullable ... hints) {

		assertInitialized();
		Assert.notNull(criteria, "Criteria must not be null");
		Integer limitOnly = resolveFindAllLimit(criteria);

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
			if (limitOnly != null) {
				query.limit(limitOnly);
			}

			return query.fetch();
		}

		// ------------------------------------------------------------
		// SAFE PATH — collection fetch allowed, ID-first + root dedup
		// ------------------------------------------------------------
		return withSinglePhase2Retry(logger, "findAll", () -> {
			JPQLQuery<ID> idQuery =
					createQuery(filter).select(idExpr);

			applySortOrDefaultById(idQuery, criteria);
			if (limitOnly != null) {
				idQuery.limit(limitOnly);
			}
			assertNoAdditionalJoins((AbstractJPAQuery<?, ?>) idQuery, "findAll.idQuery");

			List<ID> ids = idQuery.fetch();
			if (ids.isEmpty()) {
				return List.of();
			}

			Map<ID, ENTITY> unique = new LinkedHashMap<>(ids.size());

			for (List<ID> idChunk : chunk(ids, fetchInChunkSize())) {
				JPQLQuery<ENTITY> entityQuery =
						createQuery(idExpr.in(idChunk), hints).select(path);

				applyStableOrderAfterIdPaging(entityQuery, criteria, idChunk);

				List<ENTITY> rows = entityQuery.fetch();
				Map<ID, ENTITY> chunkRows = deduplicateById(rows);
				for (ENTITY entity : materializeOrderedEntities(idChunk, chunkRows, "findAll")) {
					unique.putIfAbsent(extractEntityId(entity), entity);
				}
			}

			if (unique.size() != ids.size()) {
				throw new IllegalStateException(
						"findAll phase-2 fetch returned " + unique.size() +
								" unique roots for " + ids.size() + " ids."
				);
			}

			return new ArrayList<>(unique.values());
		});
	}

	private @Nullable Integer resolveFindAllLimit(CRITERIA criteria) {
		Integer requestedLimit = criteria.getLimit();
		if (requestedLimit == null || requestedLimit <= 0) {
			return null;
		}
		return Math.min(requestedLimit, criteria.maxRowsPerPage());
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
	public Page<ENTITY> findByPaging(CRITERIA criteria, String @Nullable ... hints) {

		assertInitialized();
		Assert.notNull(criteria, "Criteria must not be null");

		Pageable pageable = criteria.toPageable();
		Assert.notNull(pageable, "Pageable must not be null");
		assertPageSizeWithinContract(criteria, pageable);

		Predicate filter = criteria.getFilter(path);

		boolean requiresIdFirst =
				fetchGraphContainsCollection(hints);
		long startedNs = System.nanoTime();

		// ------------------------------------------------------------
		// FAST PATH (to-one only)
		// ------------------------------------------------------------
		if (!requiresIdFirst) {
			long fetchStartedNs = System.nanoTime();

			JPQLQuery<ENTITY> query =
					createQuery(filter, hints).select(path);

			applySortOrDefaultById(query, criteria);

			query = query.offset(pageable.getOffset())
					.limit(pageable.getPageSize());

			List<ENTITY> content = query.fetch();
			long fetchMs = elapsedMillis(fetchStartedNs);

			long total;
			long countMs = 0L;
			if (content.size() < pageable.getPageSize()) {
				total = pageable.getOffset() + content.size();
			}
			else {
				long countStartedNs = System.nanoTime();
				total = count(criteria);
				countMs = elapsedMillis(countStartedNs);
			}

			logPagingMetrics(
					criteria,
					pageable,
					false,
					content.size(),
					content.size(),
					total,
					fetchMs,
					0L,
					countMs,
					elapsedMillis(startedNs)
			);

			return new PageImpl<>(content, pageable, total);
		}

		// ------------------------------------------------------------
		// PHASE 1 — ID PAGE (GLOBAL ORDER + OFFSET/LIMIT)
		// ------------------------------------------------------------
		long phase1StartedNs = System.nanoTime();
		JPQLQuery<ID> idQuery =
				createQuery(filter).select(idExpr);

		applySortOrDefaultById(idQuery, criteria);

		idQuery = idQuery
				.offset(pageable.getOffset())
				.limit(pageable.getPageSize());

		assertNoAdditionalJoins((AbstractJPAQuery<?, ?>) idQuery, "findByPaging.idQuery");
		List<ID> ids = idQuery.fetch();
		long phase1Ms = elapsedMillis(phase1StartedNs);

		if (ids.isEmpty()) {
			long countStartedNs = System.nanoTime();
			long total = count(criteria);
			long countMs = elapsedMillis(countStartedNs);

			logPagingMetrics(
					criteria,
					pageable,
					true,
					0,
					0,
					total,
					phase1Ms,
					0L,
					countMs,
					elapsedMillis(startedNs)
			);
			return new PageImpl<>(List.of(), pageable, total);
		}

		// ------------------------------------------------------------
		// PHASE 2 — ENTITY FETCH
		// ------------------------------------------------------------
		return withSinglePhase2Retry(logger, "findByPaging", () -> {
			long phase2StartedNs = System.nanoTime();
			JPQLQuery<ENTITY> entityQuery =
					createQuery(idExpr.in(ids), hints).select(path);

			applyStableOrderAfterIdPaging(entityQuery, criteria, ids);

			List<ENTITY> rows = entityQuery.fetch();
			Map<ID, ENTITY> unique = deduplicateById(rows);
			List<ENTITY> content = materializeOrderedEntities(ids, unique, "findByPaging");
			long phase2Ms = elapsedMillis(phase2StartedNs);

			if (content.size() != ids.size()) {
				throw new IllegalStateException(
						"Phase-2 fetch returned " + content.size() +
								" unique roots for " + ids.size() + " ids. " +
								"FetchGraph likely produced join-multiplication or filtered rows."
				);
			}

			long total;
			long countMs = 0L;
			if (ids.size() < pageable.getPageSize()) {
				total = pageable.getOffset() + ids.size();
			}
			else {
				long countStartedNs = System.nanoTime();
				total = count(criteria);
				countMs = elapsedMillis(countStartedNs);
			}

			logPagingMetrics(
					criteria,
					pageable,
					true,
					ids.size(),
					content.size(),
					total,
					phase1Ms,
					phase2Ms,
					countMs,
					elapsedMillis(startedNs)
			);

			return new PageImpl<>(content, pageable, total);
		});
	}

	/**
	 * Deterministic keyset/cursor pagination implementation.
	 *
	 * <p>
	 * What this method does:
	 * <ul>
	 *   <li>Applies normal criteria filter + cursor clause ({@code id > afterIdExclusive}).</li>
	 *   <li>Always sorts by root ID ascending for a stable continuation contract.</li>
	 *   <li>Reads {@code limit + 1} rows/ids to derive {@code hasNext} without COUNT(*).</li>
	 *   <li>Returns {@link KeysetPage} with {@code data}, {@code nextCursor}, {@code hasNext}, {@code pageSize}.</li>
	 * </ul>
	 * </p>
	 *
	 * <p>
	 * Example:
	 * <br>
	 * ids = [10, 11, 14, 15, 20], limit = 2
	 * <br>
	 * Request#1: afterIdExclusive = null  -> data=[10,11], nextCursor=11, hasNext=true
	 * <br>
	 * Request#2: afterIdExclusive = 11    -> data=[14,15], nextCursor=15, hasNext=true
	 * <br>
	 * Request#3: afterIdExclusive = 15    -> data=[20],    nextCursor=null, hasNext=false
	 * </p>
	 *
	 * <p>
	 * Why two execution paths exist:
	 * <ul>
	 *   <li>To-one fetch graph: direct entity query is safe and fastest.</li>
	 *   <li>Collection fetch graph: uses ID-first then entity-fetch-by-id to prevent join row expansion from corrupting pages.</li>
	 * </ul>
	 * This is the core guard against:
	 * <ul>
	 *   <li>Hibernate in-memory paging</li>
	 *   <li>Duplicate entities</li>
	 *   <li>Missing records</li>
	 *   <li>Broken pagination with joins</li>
	 * </ul>
	 * </p>
	 *
	 * <p>
	 * Guarantees:
	 * <ul>
	 *   <li>Cursor is based on root primary-key order only.</li>
	 *   <li>Deterministic continuation with no offset drift for deep traversal.</li>
	 *   <li>No total-count query overhead.</li>
	 * </ul>
	 * </p>
	 *
	 * <p>
	 * Design choice: keyset paging is intentionally ID-ordered only.
	 * Supporting arbitrary ORDER BY safely would require compound cursor encoding
	 * and strict tie-break contracts; that complexity is intentionally excluded here
	 * to preserve correctness guarantees.
	 * </p>
	 */
	@Override
	public KeysetPage<ENTITY, ID> findByKeyset(
			CRITERIA criteria,
			@Nullable ID afterIdExclusive,
			@Nullable Integer limit,
			String @Nullable ... hints) {

		assertInitialized();
		Assert.notNull(criteria, "Criteria must not be null");

		// Keyset limit uses criteria contracts and shares the same max-row protection
		// as offset paging to avoid oversized cursor pages.
		int resolvedLimit = resolveKeysetLimit(criteria, limit);
		BooleanBuilder keysetFilter = new BooleanBuilder();
		keysetFilter.and(criteria.getFilter(path));
		if (afterIdExclusive != null) {
			keysetFilter.and(idComparableExpr.gt(afterIdExclusive));
		}

		Predicate effectiveFilter = keysetFilter.hasValue() ? keysetFilter : null;
		boolean requiresIdFirst = fetchGraphContainsCollection(hints);

		// ------------------------------------------------------------
		// FAST PATH (to-one only)
		// ------------------------------------------------------------
		if (!requiresIdFirst) {
			JPQLQuery<ENTITY> query =
					createQuery(effectiveFilter, hints).select(path);

			// Fetch one extra row to compute hasNext without COUNT(*).
			query.orderBy(idAscOrder)
					.limit((long) resolvedLimit + 1L);

			List<ENTITY> rows = query.fetch();

			Map<ID, ENTITY> unique = deduplicateById(rows);
			if (unique.size() != rows.size()) {
				throw new IllegalStateException(
						"findByKeyset() direct fetch returned duplicate roots. " +
								"Use collection fetch graph ID-first path."
				);
			}

			boolean hasNext = rows.size() > resolvedLimit;
			List<ENTITY> content =
					hasNext ? rows.subList(0, resolvedLimit) : rows;

			ID nextCursor =
					hasNext ? extractEntityId(content.getLast()) : null;

			return new KeysetPage<>(content, nextCursor, hasNext, resolvedLimit);
		}

		// ------------------------------------------------------------
		// SAFE PATH (collection graph) — ID first + deterministic fetch
		// ------------------------------------------------------------
		JPQLQuery<ID> idQuery =
				createQuery(effectiveFilter).select(idExpr);

		// Same +1 strategy on ID phase keeps cursor metadata exact and cheap.
		idQuery.orderBy(idAscOrder)
				.limit((long) resolvedLimit + 1L);

		assertNoAdditionalJoins((AbstractJPAQuery<?, ?>) idQuery, "findByKeyset.idQuery");
		List<ID> fetchedIds = idQuery.fetch();

		if (fetchedIds.isEmpty()) {
			return new KeysetPage<>(List.of(), null, false, resolvedLimit);
		}

		boolean hasNext = fetchedIds.size() > resolvedLimit;
		List<ID> pageIds = hasNext
				? new ArrayList<>(fetchedIds.subList(0, resolvedLimit))
				: fetchedIds;

		return withSinglePhase2Retry(logger, "findByKeyset", () -> {
			JPQLQuery<ENTITY> entityQuery =
					createQuery(idExpr.in(pageIds), hints).select(path);

			applyKeysetOrder(entityQuery, pageIds);

			List<ENTITY> rows = entityQuery.fetch();
			Map<ID, ENTITY> unique = deduplicateById(rows);
			List<ENTITY> content = materializeOrderedEntities(pageIds, unique, "findByKeyset");

			if (content.size() != pageIds.size()) {
				throw new IllegalStateException(
						"findByKeyset() phase-2 fetch returned " + content.size() +
								" unique roots for " + pageIds.size() + " ids."
				);
			}

			ID nextCursor = hasNext ? pageIds.getLast() : null;
			return new KeysetPage<>(content, nextCursor, hasNext, resolvedLimit);
		});
	}

	/**
	 * Resolves keyset page size with deterministic safety bounds.
	 *
	 * <p>
	 * Fallback order:
	 * <ol>
	 *   <li>caller-provided limit</li>
	 *   <li>criteria default page size</li>
	 *   <li>repository fallback constant</li>
	 * </ol>
	 * Then enforce {@link AbstractCriteria#maxRowsPerPage()} strictly.
	 * </p>
	 */
	private int resolveKeysetLimit(CRITERIA criteria, @Nullable Integer requestedLimit) {

		int limit = requestedLimit == null || requestedLimit <= 0
				? criteria.defaultRowsPerPage()
				: requestedLimit;

		if (limit <= 0) {
			limit = DEFAULT_KEYSET_LIMIT;
		}

		int maxAllowed = criteria.maxRowsPerPage();
		if (limit > maxAllowed) {
			throw new IllegalStateException(
					"Invalid keyset limit " + limit + ". Maximum allowed is " + maxAllowed + "."
			);
		}

		return limit;
	}

	private void applyKeysetOrder(JPQLQuery<?> query, List<ID> ids) {
		if (isPostgresDb()) {
			query.orderBy(idAscOrder);
			return;
		}
		applyIdOrder(query, ids);
	}

	/**
	 * Extracts strongly-typed ID from entity for cursor emission.
	 */
	private ID extractEntityId(ENTITY entity) {
		@SuppressWarnings("unchecked")
		ID id = (ID) entity.getId();
		return id;
	}

	/**
	 * Enforces page size contract before any query is issued.
	 *
	 * <p>
	 * This protects both correctness (bounded deterministic SQL shape)
	 * and system stability (prevents large, accidental page allocations).
	 * </p>
	 */
	protected void assertPageSizeWithinContract(CRITERIA criteria, Pageable pageable) {

		int requestedSize = pageable.getPageSize();
		if (requestedSize <= 0) {
			throw new IllegalStateException(
					"Invalid page size " + requestedSize + ". Page size must be > 0."
			);
		}

		int maxAllowed = criteria.maxRowsPerPage();
		if (requestedSize > maxAllowed) {
			throw new IllegalStateException(
					"Invalid page size " + requestedSize + ". " +
							"Maximum allowed is " + maxAllowed + "."
			);
		}
	}

	private long elapsedMillis(long startedNs) {
		return (System.nanoTime() - startedNs) / 1_000_000L;
	}

	/**
	 * Structured debug-level timings for pagination phases.
	 *
	 * <p>
	 * Intended for production diagnostics of slow pages and plan regressions
	 * without enabling SQL trace logs globally.
	 * </p>
	 */
	private void logPagingMetrics(
			CRITERIA criteria,
			Pageable pageable,
			boolean idFirst,
			int idsSize,
			int rowsSize,
			long total,
			long phase1Ms,
			long phase2Ms,
			long countMs,
			long totalMs) {

		if (!logger.isDebugEnabled()) {
			return;
		}

		logger.debug(
				"paging domain={} criteria={} mode={} offset={} limit={} ids={} rows={} total={} phase1Ms={} phase2Ms={} countMs={} totalMs={}",
				domainClass.getSimpleName(),
				criteria.getClass().getSimpleName(),
				idFirst ? "id-first" : "direct",
				pageable.getOffset(),
				pageable.getPageSize(),
				idsSize,
				rowsSize,
				total,
				phase1Ms,
				phase2Ms,
				countMs,
				totalMs
		);
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

		assertInitialized();
		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		JPQLQuery<ID> query =
				createQuery(filter).select(idExpr);

		applySortOrDefaultById(query, criteria);
		assertNoAdditionalJoins((AbstractJPAQuery<?, ?>) query, "findIds");

		return query.fetch();
	}

	protected List<ID> findIdsForBulk(CRITERIA criteria) {

		assertInitialized();
		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		JPQLQuery<ID> query =
				createQuery(filter).select(idExpr);

		// Bulk operations do not require caller-specified sorting.
		// Use deterministic PK ordering to keep chunk processing stable and index-friendly.
		query.orderBy(idAscOrder);
		assertNoAdditionalJoins((AbstractJPAQuery<?, ?>) query, "findIdsForBulk");

		return query.fetch();
	}

	protected List<ID> fetchNextBulkIds(
			CRITERIA criteria,
			@Nullable ID afterIdExclusive,
			int limit
	) {

		assertInitialized();
		Assert.notNull(criteria, "Criteria must not be null");
		Assert.isTrue(limit > 0, "limit must be positive");

		BooleanBuilder filter = new BooleanBuilder();
		filter.and(criteria.getFilter(path));
		if (afterIdExclusive != null) {
			filter.and(idComparableExpr.gt(afterIdExclusive));
		}

		JPQLQuery<ID> query =
				createQuery(filter.hasValue() ? filter : null).select(idExpr);

		query.orderBy(idAscOrder)
				.limit(limit);
		assertNoAdditionalJoins((AbstractJPAQuery<?, ?>) query, "fetchNextBulkIds");

		return query.fetch();
	}

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

		assertInitialized();
		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		AbstractJPAQuery<?, ?> query = createQuery(filter);
		assertNoAdditionalJoins(query, "count");

		Long count =
				query.select(idExpr.count())
						.fetchOne();

		return count == null ? 0L : count;
	}

	// ----------------------------------------------------------------------
	// ID / COUNT / EXISTS
	// ----------------------------------------------------------------------

	/**
	 * Existence check.
	 */
	@Override
	public boolean exists(CRITERIA criteria) {

		assertInitialized();
		Assert.notNull(criteria, "Criteria must not be null");

		Predicate filter = criteria.getFilter(path);

		AbstractJPAQuery<?, ?> query = createQuery(filter);
		assertNoAdditionalJoins(query, "exists");

		return query
				.select(idExpr)
				.fetchFirst() != null;
	}

	/**
	 * Encapsulates deterministic data access for `existsById` so callers do not duplicate query logic across services.
	 *
	 * @param id input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Override
	public boolean existsById(ID id) {

		assertInitialized();
		Assert.notNull(id, "Id must not be null");

		// root entity path only
		BooleanBuilder predicate = new BooleanBuilder();
		predicate.and(idExpr.eq(id));

		AbstractJPAQuery<?, ?> query = createQuery(predicate);

		// enforce aggregate boundary
		assertNoAdditionalJoins(query, "existsById");

		return query
				.select(idExpr)
				.fetchFirst() != null;
	}

	/**
	 * Encapsulates deterministic data access for `saveRecord` so callers do not duplicate query logic across services.
	 *
	 * @param entity input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Override
	public ENTITY saveRecord(ENTITY entity) {
		assertInitialized();
		Assert.notNull(entity, "Entity must not be null");

		ENTITY persisted;
		if (entity.getId() == null) {
			entityManager.persist(entity);
			persisted = entity;
		}
		else {
			persisted = entityManager.merge(entity);
		}
		entityManager.flush();
		return persisted;
	}

	// ----------------------------------------------------------------------
	// WRITE OPERATIONS
	// ----------------------------------------------------------------------

	/**
	 * Encapsulates deterministic data access for `saveAllRecords` so callers do not duplicate query logic across services.
	 *
	 * @param entities input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Override
	public List<ENTITY> saveAllRecords(Iterable<ENTITY> entities) {
		assertInitialized();
		Assert.notNull(entities, "Entities must not be null");

		List<ENTITY> saved = new ArrayList<>();
		int batchSize = bulkInChunkSize();
		int processed = 0;
		for (ENTITY entity : entities) {
			Assert.notNull(entity, "Entity element must not be null");
			if (entity.getId() == null) {
				entityManager.persist(entity);
				saved.add(entity);
			}
			else {
				saved.add(entityManager.merge(entity));
			}
			processed++;
			if (processed % batchSize == 0) {
				entityManager.flush();
				entityManager.clear();
			}
		}
		if (processed % batchSize != 0) {
			entityManager.flush();
			entityManager.clear();
		}
		return saved;
	}

	/**
	 * Encapsulates deterministic data access for `updateById` so callers do not duplicate query logic across services.
	 *
	 * @param spec      input required by this operation contract
	 * @param id        input required by this operation contract
	 * @param updatedBy input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Override
	public long updateById(
			UpdateSpec<ENTITY> spec,
			ID id,
			long updatedBy) {

		assertInitialized();
		Assert.notNull(spec, "UpdateSpec must not be null");

		JPAUpdateClause update = queryFactory.update(path);

		applyAudit(update, updatedBy);

		spec.apply(update, path);

		long affected = update.where(idExpr.eq(id)).execute();

		afterBulkDml();

		return affected;
	}

	/**
	 * Encapsulates deterministic data access for `updateByCriteria` so callers do not duplicate query logic across services.
	 *
	 * @param spec      input required by this operation contract
	 * @param criteria  input required by this operation contract
	 * @param updatedBy input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Override
	public <E extends ENTITY> long updateByCriteria(
			UpdateSpec<E> spec,
			CRITERIA criteria,
			Long updatedBy) {

		assertInitialized();
		Assert.notNull(criteria, "Criteria must not be null");
		Assert.notNull(spec, "UpdateSpec must not be null");

		@SuppressWarnings("unchecked")
		EntityPathBase<E> typedPath =
				(EntityPathBase<E>) path;

		long affected = 0;
		ID afterIdExclusive = null;
		Predicate criteriaFilter = criteria.getFilter(path);
		int chunkSize = bulkInChunkSize();

		while (true) {
			List<ID> chunk = fetchNextBulkIds(criteria, afterIdExclusive, chunkSize);
			if (chunk.isEmpty()) {
				break;
			}

			JPAUpdateClause update =
					queryFactory.update(typedPath);

			applyAudit(update, updatedBy);
			spec.apply(update, typedPath);

			BooleanBuilder chunkFilter = new BooleanBuilder();
			chunkFilter.and(idExpr.in(chunk));
			chunkFilter.and(criteriaFilter);
			affected += update.where(chunkFilter).execute();
			afterIdExclusive = chunk.getLast();
		}

		afterBulkDml();

		return affected;
	}

	// ----------------------------------------------------------------------
	// BULK OPERATIONS (ID-FIRST, SAFE)
	// ----------------------------------------------------------------------

	/**
	 * Encapsulates deterministic data access for `deleteWithId` so callers do not duplicate query logic across services.
	 *
	 * @param id input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Override
	public boolean deleteWithId(ID id) {

		assertInitialized();
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

	/**
	 * Encapsulates deterministic data access for `deleteByCriteria` so callers do not duplicate query logic across services.
	 *
	 * @param criteria input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Override
	public long deleteByCriteria(CRITERIA criteria) {

		assertInitialized();
		Assert.notNull(criteria, "Criteria must not be null");

		long affected = 0;
		ID afterIdExclusive = null;
		Predicate criteriaFilter = criteria.getFilter(path);
		int chunkSize = bulkInChunkSize();

		while (true) {
			List<ID> chunk = fetchNextBulkIds(criteria, afterIdExclusive, chunkSize);
			if (chunk.isEmpty()) {
				break;
			}
			BooleanBuilder chunkFilter = new BooleanBuilder();
			chunkFilter.and(idExpr.in(chunk));
			chunkFilter.and(criteriaFilter);
			affected += queryFactory
					.delete(path)
					.where(chunkFilter)
					.execute();
			afterIdExclusive = chunk.getLast();
		}

		afterBulkDml();

		return affected;
	}

	protected AbstractJPAQuery<?, ?> createQuery(
			@Nullable Predicate predicate,
			String @Nullable ... hints) {

		AbstractJPAQuery<?, ?> query = queryFactory.selectFrom(path);

		if (predicate != null) {
			query.where(predicate);
		}

		QueryHints qh = getRelatedDataHints(hints);
		if (qh != null) {
			qh.forEach(query::setHint);
		}

		return query;
	}

	protected void assertNoAdditionalJoins(AbstractJPAQuery<?, ?> query, String context) {

		AbstractJPAQuery<?, ?> safeQuery = Objects.requireNonNull(query, "Query must not be null");
		String safeContext = Objects.requireNonNull(context, "Context must not be null");
		List<JoinExpression> joins = Objects.requireNonNull(
				Objects.requireNonNull(safeQuery.getMetadata(), "Query metadata must not be null").getJoins(),
				"Query joins must not be null"
		);
		if (joins.isEmpty()) {
			return;
		}

		// The first join is the root entity (FROM). Any additional join is unsafe here.
		if (joins.size() > 1) {
			throw new IllegalStateException(
					"Unsafe join detected in " + safeContext + ". " +
							"ID/count queries must not introduce additional joins. joins=" + joins
			);
		}

		JoinExpression root = joins.getFirst();
		if (!Objects.equals(root.getTarget(), path)) {
			throw new IllegalStateException(
					"Unexpected join root in " + safeContext + ". Expected " + path + " but found " + root.getTarget()
			);
		}
	}

	protected final void afterBulkDml() {
		entityManager.flush();
		entityManager.clear();
	}

	protected void applyAudit(
			JPAUpdateClause update,
			Long updatedBy) {

		JPAUpdateClause safeUpdate = Objects.requireNonNull(update, "Update clause must not be null");
		safeUpdate.set(audit.updatedDate, LocalDateTime.now());
		safeUpdate.set(audit.updatedBy, updatedBy);
	}

	// ----------------------------------------------------------------------
	// QUERY CONSTRUCTION
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
			validateSortPathSafety(target, o);

			// 2) reject non-orderable scalar types (JSON/BLOB/embeddable/etc.)
			validateOrderableType(target, o);
		}
	}

	private void validateSortPathSafety(Expression<?> target, OrderSpecifier<?> o) {

		if (containsCollectionPath(target)) {
			throw new IllegalStateException(
					"Unsafe ORDER BY detected: sorting on collection-valued " +
							"association is not pagination-safe.\n" +
							"OrderSpecifier=" + o
			);
		}

		if (!(target instanceof Path<?> targetPath)) {
			return;
		}

		List<String> segments = extractOrderPathSegments(targetPath);
		if (segments.isEmpty()) {
			return;
		}

		ManagedType<?> current =
				rootManagedType();

		for (int i = 0; i < segments.size(); i++) {

			String segment = segments.get(i);
			Attribute<?, ?> attr = tryGetAttribute(current, segment);

			if (attr == null) {
				throw new IllegalStateException(
						"Unsafe ORDER BY target: invalid path segment '" + segment + "' " +
								"for entity " + current.getJavaType().getSimpleName() + " in " + o
				);
			}

			if (attr.isCollection()) {
				throw new IllegalStateException(
						"Unsafe ORDER BY detected: sorting on collection-valued " +
								"association is not pagination-safe.\n" +
								"OrderSpecifier=" + o
				);
			}

			boolean last = i == segments.size() - 1;
			if (last) {
				return;
			}

			if (attr instanceof SingularAttribute<?, ?> sa
					&& sa.getType() instanceof ManagedType<?> mt) {
				current = mt;
				continue;
			}

			throw new IllegalStateException(
					"Unsafe ORDER BY target: path traverses non-entity attribute '" +
							segment + "' in " + o
			);
		}
	}

	private void validateOrderableType(Expression<?> target, OrderSpecifier<?> o) {

		// QueryDSL often represents truly orderable paths as ComparableExpressionBase.
		// But we still enforce a strict type rule to protect from custom paths.
		if (target instanceof Path<?>) {

			if (!(target instanceof ComparableExpressionBase<?>)) {

				Class<?> t = target.getType();
				boolean ok = Comparable.class.isAssignableFrom(t) || t.isEnum() || t.isPrimitive();
				if (!ok) {
					throw new IllegalStateException(
							"Unsafe ORDER BY target: non-orderable type " + t.getName() + " for " + o
					);
				}
			}
		}

		Class<?> type = target.getType();
		if (type.isPrimitive() || type.isEnum() || Comparable.class.isAssignableFrom(type)) {
			return;
		}

		throw new IllegalStateException(
				"Unsafe ORDER BY on non-Comparable type: " + type.getName()
						+ " for " + o
		);
	}

	private @Nullable Attribute<?, ?> tryGetAttribute(ManagedType<?> type, String attrName) {
		try {
			return type.getAttribute(attrName);
		}
		catch (IllegalArgumentException ignored) {
			return null;
		}
	}

	private List<String> extractOrderPathSegments(Path<?> targetPath) {

		LinkedList<String> segments = new LinkedList<>();
		Path<?> cursor = targetPath;

		while (cursor != null) {
			PathMetadata md = cursor.getMetadata();
			String name = md.getName();
			if (!name.isBlank()) {
				segments.addFirst(name);
			}

			cursor = md.getParent();
		}

		PathMetadata rootMd = path.getMetadata();
		String rootName = rootMd.getName();

		if (!rootName.isBlank() && !segments.isEmpty() && rootName.equals(segments.getFirst())) {
			segments.removeFirst();
		}

		return segments;
	}

	private boolean containsCollectionPath(Expression<?> expr) {

		if (expr instanceof CollectionPathBase<?, ?, ?>) {
			return true;
		}

		if (expr instanceof Path<?> p) {
			PathMetadata md = p.getMetadata();
			if (md.getParent() != null) {
				return containsCollectionPath(md.getParent());
			}
		}

		return false;
	}

	protected void applySort(JPQLQuery<?> query, CRITERIA criteria) {

		List<OrderSpecifier<?>> orderSpecifiers =
				criteria.resolveOrderSpecifiers(path);

		if (orderSpecifiers.isEmpty()) {
			return;
		}

		// 🔒 STRICT FAIL FAST — prevent pagination-unsafe sorting
		validateSortSafety(orderSpecifiers);

		query.orderBy(orderSpecifiers.toArray(new OrderSpecifier<?>[0]));
	}

	// ----------------------------------------------------------------------
	// SORT SAFETY ENFORCEMENT (STRICT – FAIL FAST)
	// ----------------------------------------------------------------------

	protected void applySortOrDefaultById(
			JPQLQuery<?> query,
			CRITERIA criteria) {

		List<OrderSpecifier<?>> specs =
				criteria.resolveOrderSpecifiers(path);

		// No sort provided → deterministic default
		if (specs.isEmpty()) {
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

		if (isPostgresDb()) {
			// PostgreSQL: reapply ORDER BY safely (same as phase 1)
			applySortOrDefaultById(entityQuery, criteria);
			return;
		}

		// Non-Postgres: preserve phase-1 ID order explicitly
		applyIdOrder(entityQuery, ids);
	}

	protected int bulkInChunkSize() {
		return effectiveBulkInChunkSize;
	}

	protected int fetchInChunkSize() {
		// For non-Postgres we must keep fetch chunk <= CASE-order bound.
		return isPostgresDb()
				? effectiveFetchInChunkSize
				: Math.min(effectiveFetchInChunkSize, maxCaseOrderIds());
	}

	protected boolean isPostgresDb() {
		return effectivePostgresDb;
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

		if (ids.isEmpty()) {
			return;
		}

		if (ids.size() > maxCaseOrderIds()) {
			throw new IllegalStateException(
					"ID-order CASE too large (" + ids.size() + "). " +
							"Reduce page size or use PostgreSQL ordering mode."
			);
		}

		CaseBuilder cb = new CaseBuilder();
		CaseBuilder.Cases<Integer, NumberExpression<Integer>> cases =
				cb.when(idExpr.eq(ids.getFirst())).then(0);

		for (int index = 1; index < ids.size(); index++) {
			ID id = ids.get(index);
			cases = cases.when(idExpr.eq(id)).then(index);
		}
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
		return effectiveMaxCaseOrderIds;
	}

	// ----------------------------------------------------------------------
	// STABLE ORDERING AFTER ID PAGING
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
	protected boolean fetchGraphContainsCollection(String @Nullable ... hints) {

		if (hints == null || hints.length == 0) {
			return false;
		}
		ManagedType<?> managedType = rootManagedType();
		Class<?> managedJavaType = Objects.requireNonNull(
				managedType.getJavaType(),
				"Managed Java type must not be null"
		);

		for (String hint : hints) {
			String normalizedHint = Objects.toString(hint, "");
			if (normalizedHint.isBlank()) {
				continue;
			}

			String spec = normalizeGraphSpec(normalizedHint, rootEntitySimpleName);
			if (spec.isBlank()) {
				continue;
			}

			String cacheKey =
					managedJavaType.getName() + "|" + spec;

			// Cache parsed collection detection because graph hints are typically
			// repeated across requests (same controller/service endpoints).
			boolean containsCollection =
					computeCached(
							collectionFetchGraphCache,
							cacheKey,
							ignored -> containsCollection(managedType, spec)
					);

			if (containsCollection) {
				return true;
			}
		}

		return false;
	}

	private boolean containsCollection(
			ManagedType<?> type,
			@Nullable String spec) {

		if (spec == null || spec.isBlank()) {
			return false;
		}

		for (String token : splitTopLevel(spec)) {

			if (token.isEmpty()) {
				continue;
			}

			int paren = token.indexOf('(');

			if (paren < 0) {
				Attribute<?, ?> attr = validateAttribute(type, token);
				if (attr.isCollection()) {
					return true;
				}
				continue;
			}

			String attrName = token.substring(0, paren).trim();
			String nested = token.substring(paren + 1, token.length() - 1).trim();

			Attribute<?, ?> attr = validateAttribute(type, attrName);

			if (attr.isCollection()) {
				return true;
			}

			ManagedType<?> nestedType = resolveManagedType(attr);
			if (containsCollection(nestedType, nested)) {
				return true;
			}
		}

		return false;
	}

	private List<String> splitTopLevel(@Nullable String spec) {

		if (spec == null || spec.isBlank()) {
			return List.of();
		}

		// Top-level tokenization is frequently reused by graph parsing and
		// collection detection; cache it to avoid repeated string scanning.
		return computeCached(
				topLevelGraphTokenCache,
				spec,
				spec1 -> spec1 != null ? splitTopLevelUncached(spec1) : null
		);
	}

	private List<String> splitTopLevelUncached(String spec) {

		List<String> result = new ArrayList<>();
		int depth = 0;
		int start = 0;

		for (int i = 0; i < spec.length(); i++) {
			char c = spec.charAt(i);

			if (c == '(') {
				depth++;
			}
			else if (c == ')') {
				depth--;
			}
			else if (c == ',' && depth == 0) {
				result.add(spec.substring(start, i).trim());
				start = i + 1;
			}
		}

		if (start < spec.length()) {
			result.add(spec.substring(start).trim());
		}

		return List.copyOf(result);
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
			catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
				logger.error("Failed to resolve audit path", e);
			}
		}

		throw new IllegalStateException(
				"QAbstractEntity audit path not resolvable"
		);
	}

	// ----------------------------------------------------------------------
	// FETCH GRAPH INTROSPECTION (STRICT TRIGGER ONLY)
	// ----------------------------------------------------------------------

	protected interface GraphContainer {
		void addAttribute(String attribute);

		Subgraph<?> addSubgraph(Attribute<?, ?> attr, ManagedType<?> nestedType);
	}

	static final class Phase2EntityMissException extends IllegalStateException {

		@Serial
		private static final long serialVersionUID = -7520742867818193375L;
		private final String operation;
		private final List<?> missingIds;

		Phase2EntityMissException(String operation, List<?> missingIds) {
			super(operation + " phase-2 fetch missed selected root ids=" + missingIds);
			this.operation = operation;
			this.missingIds = List.copyOf(missingIds);
		}

		String operation() {
			return operation;
		}

		List<?> missingIds() {
			return missingIds;
		}
	}

	protected static final class EntityGraphContainer implements GraphContainer {
		private final EntityGraph<?> delegate;

		EntityGraphContainer(EntityGraph<?> delegate) {
			this.delegate = delegate;
		}

		/**
		 * Encapsulates deterministic data access for `addAttribute` so callers do not duplicate query logic across services.
		 *
		 * @param attribute input required by this operation contract
		 */
		@Override
		public void addAttribute(String attribute) {
			delegate.addAttributeNodes(attribute);
		}

		/**
		 * Encapsulates deterministic data access for `addSubgraph` so callers do not duplicate query logic across services.
		 *
		 * @param attr       input required by this operation contract
		 * @param nestedType input required by this operation contract
		 * @return result required by downstream orchestration logic
		 */
		@Override
		public Subgraph<?> addSubgraph(Attribute<?, ?> attr, ManagedType<?> nestedType) {
			return delegate.addSubgraph(attr.getName(), nestedType.getJavaType());
		}
	}

	// ----------------------------------------------------------------------
	// AUDIT PATH RESOLUTION
	// ----------------------------------------------------------------------

	protected static final class SubgraphContainer implements GraphContainer {
		private final Subgraph<?> delegate;

		SubgraphContainer(Subgraph<?> delegate) {
			this.delegate = delegate;
		}

		/**
		 * Encapsulates deterministic data access for `addAttribute` so callers do not duplicate query logic across services.
		 *
		 * @param attribute input required by this operation contract
		 */
		@Override
		public void addAttribute(String attribute) {
			delegate.addAttributeNodes(attribute);
		}

		/**
		 * Encapsulates deterministic data access for `addSubgraph` so callers do not duplicate query logic across services.
		 *
		 * @param attr       input required by this operation contract
		 * @param nestedType input required by this operation contract
		 * @return result required by downstream orchestration logic
		 */
		@Override
		public Subgraph<?> addSubgraph(Attribute<?, ?> attr, ManagedType<?> nestedType) {
			return delegate.addSubgraph(attr.getName(), nestedType.getJavaType());
		}
	}
}

