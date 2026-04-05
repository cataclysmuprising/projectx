package com.github.projectx.persistence.criteria.base;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.github.projectx.persistence.entity.base.QAbstractEntity;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.CollectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Data
public abstract class AbstractCriteria<A extends EntityPathBase<?>> {

	// ----------------------------------------------------------------------
	// DEFAULTS
	// ----------------------------------------------------------------------

	protected static final int DEFAULT_PAGE_SIZE = 20;
	protected static final int DEFAULT_MAX_ROWS = 200;
	private static final String FLEXIBLE_DATE_TIME_PATTERN =
			"[dd-MM-yyyy HH:mm:ss][yyyy-MM-dd HH:mm:ss][yyyy-MM-dd'T'HH:mm:ss]";
	/**
	 * Explicit QueryDSL sort specifiers (highest priority).
	 */
	protected final List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
	protected Set<Long> includeIds;

	// ----------------------------------------------------------------------
	// COMMON FILTER FIELDS
	// ----------------------------------------------------------------------
	protected Set<Long> excludeIds;
	protected Long id;
	protected Long createdBy;
	protected Long updatedBy;
	@JsonFormat(pattern = FLEXIBLE_DATE_TIME_PATTERN)
	@DateTimeFormat(pattern = FLEXIBLE_DATE_TIME_PATTERN)
	protected LocalDateTime createdDateFrom;
	@JsonFormat(pattern = FLEXIBLE_DATE_TIME_PATTERN)
	@DateTimeFormat(pattern = FLEXIBLE_DATE_TIME_PATTERN)
	protected LocalDateTime createdDateTo;
	@JsonFormat(pattern = FLEXIBLE_DATE_TIME_PATTERN)
	@DateTimeFormat(pattern = FLEXIBLE_DATE_TIME_PATTERN)
	protected LocalDateTime updatedDateFrom;
	@JsonFormat(pattern = FLEXIBLE_DATE_TIME_PATTERN)
	@DateTimeFormat(pattern = FLEXIBLE_DATE_TIME_PATTERN)
	protected LocalDateTime updatedDateTo;
	protected String keyword;
	/**
	 * Client / Java provided string-based sorting.
	 * Must match 1:1 with {@link #sortDirs}.
	 */
	protected List<String> sortKeys;
	protected List<Sort.Direction> sortDirs;
	protected Integer pageNumber; // 1-based
	protected Integer offset;     // absolute offset

	// ----------------------------------------------------------------------
	// PAGING INPUTS
	// ----------------------------------------------------------------------
	protected Integer limit;

	/**
	 * Default logical page size for caller-facing pagination APIs.
	 *
	 * <p>
	 * Used by offset paging and keyset paging when caller does not provide a limit.
	 * Keeping this in criteria makes the limit contract discoverable and override-friendly
	 * at domain criteria level.
	 * </p>
	 */
	public final int defaultRowsPerPage() {
		return DEFAULT_PAGE_SIZE;
	}

	/**
	 * Hard upper bound for rows returned by a single page request.
	 *
	 * <p>
	 * This protects deterministic paging paths from pathological request sizes
	 * and bounds SQL complexity (IN lists, CASE ordering, memory pressure).
	 * </p>
	 */
	public final int maxRowsPerPage() {
		return DEFAULT_MAX_ROWS;
	}

	// ----------------------------------------------------------------------
	// COMMON FILTER LOGIC
	// ----------------------------------------------------------------------

	protected BooleanBuilder commonFilter(QAbstractEntity audit) {

		BooleanBuilder predicate = new BooleanBuilder();

		if (id != null) {
			predicate.and(audit.id.eq(id));
		}
		if (createdBy != null) {
			predicate.and(audit.createdBy.eq(createdBy));
		}
		if (updatedBy != null) {
			predicate.and(audit.updatedBy.eq(updatedBy));
		}
		if (createdDateFrom != null) {
			predicate.and(audit.createdDate.goe(createdDateFrom));
		}
		if (createdDateTo != null) {
			predicate.and(audit.createdDate.loe(createdDateTo));
		}
		if (updatedDateFrom != null) {
			predicate.and(audit.updatedDate.goe(updatedDateFrom));
		}
		if (updatedDateTo != null) {
			predicate.and(audit.updatedDate.loe(updatedDateTo));
		}
		if (!CollectionUtils.isEmpty(includeIds)) {
			predicate.and(audit.id.in(includeIds));
		}
		if (!CollectionUtils.isEmpty(excludeIds)) {
			predicate.and(audit.id.notIn(excludeIds));
		}

		return predicate;
	}

	// ----------------------------------------------------------------------
	// SORTING – JAVA SIDE (TYPE SAFE)
	// ----------------------------------------------------------------------

	/**
	 * Accepts typed QueryDSL sort expressions so domain criteria can enforce deterministic ordering
	 * without exposing stringly-typed sort keys.
	 */
	public void addSort(
			ComparableExpressionBase<? extends Comparable<?>> property,
			Sort.Direction direction
	) {
		if (property == null || direction == null) {
			return;
		}
		orderSpecifiers.add(
				new OrderSpecifier<>(
						direction.isAscending() ? Order.ASC : Order.DESC,
						property
				)
		);
	}

	// ----------------------------------------------------------------------
	// SORTING – STRING SIDE (CLIENT)
	// ----------------------------------------------------------------------

	/**
	 * Keeps mutable criteria configuration in one place so query construction remains explicit and traceable.
	 */
	public void addSortKey(String key, Sort.Direction direction) {
		if (key == null || direction == null) {
			return;
		}
		if (sortKeys == null) {
			sortKeys = new ArrayList<>();
			sortDirs = new ArrayList<>();
		}
		sortKeys.add(key);
		sortDirs.add(direction);
	}

	/**
	 * Keeps mutable criteria configuration in one place so query construction remains explicit and traceable.
	 */
	public void clearStringSorts() {
		if (sortKeys != null) {
			sortKeys.clear();
		}
		if (sortDirs != null) {
			sortDirs.clear();
		}
	}

	// ----------------------------------------------------------------------
	// SORT RESOLUTION – QUERYDSL (DB)
	// ----------------------------------------------------------------------

	/**
	 * Centralizes resolution rules so callers do not duplicate sorting or paging decisions in multiple layers.
	 */
	public List<OrderSpecifier<?>> resolveOrderSpecifiers(EntityPathBase<?> root) {

		// 1️⃣ Explicit Java-side sorting
		if (!orderSpecifiers.isEmpty()) {
			return List.copyOf(orderSpecifiers);
		}

		// 2️⃣ String-based sorting
		if (root != null
				&& !CollectionUtils.isEmpty(sortKeys)
				&& !CollectionUtils.isEmpty(sortDirs)) {

			if (sortKeys.size() != sortDirs.size()) {
				throw new IllegalStateException(
						"sortKeys and sortDirs size mismatch"
				);
			}

			PathBuilder<?> pb =
					new PathBuilder<>(root.getType(), root.getMetadata());

			List<OrderSpecifier<?>> orders = new ArrayList<>();

			for (int i = 0; i < sortKeys.size(); i++) {
				ComparableExpressionBase<?> expr =
						resolveComparablePath(pb, sortKeys.get(i));
				orders.add(
						new OrderSpecifier<>(
								sortDirs.get(i).isAscending() ? Order.ASC : Order.DESC,
								expr
						)
				);
			}

			if (!orders.isEmpty()) {
				return orders;
			}
		}

		// 3️⃣ Deterministic fallback
		if (root == null) {
			return Collections.emptyList();
		}

		PathBuilder<?> pb =
				new PathBuilder<>(root.getType(), root.getMetadata());

		return List.of(
				new OrderSpecifier<>(
						Order.DESC,
						pb.getComparable("id", Comparable.class)
				)
		);
	}

	public boolean hasExplicitSorting() {
		return !orderSpecifiers.isEmpty() || !CollectionUtils.isEmpty(sortKeys);
	}

	protected ComparableExpressionBase<?> resolveComparablePath(
			PathBuilder<?> root,
			String key
	) {
		String[] parts = key.split("\\.");
		PathBuilder<?> pb = root;

		for (int i = 0; i < parts.length - 1; i++) {
			pb = pb.get(parts[i], Object.class);
		}
		return pb.getComparable(parts[parts.length - 1], Comparable.class);
	}

	// ----------------------------------------------------------------------
	// SPRING DATA SORT (PAGING)
	// ----------------------------------------------------------------------

	/**
	 * Centralizes resolution rules so callers do not duplicate sorting or paging decisions in multiple layers.
	 */
	public Sort resolveSort() {

		if (!CollectionUtils.isEmpty(sortKeys)
				&& !CollectionUtils.isEmpty(sortDirs)) {

			if (sortKeys.size() != sortDirs.size()) {
				throw new IllegalStateException(
						"sortKeys and sortDirs size mismatch"
				);
			}

			List<Sort.Order> orders = new ArrayList<>();
			for (int i = 0; i < sortKeys.size(); i++) {
				orders.add(
						new Sort.Order(sortDirs.get(i), sortKeys.get(i))
				);
			}
			return Sort.by(orders);
		}

		return Sort.by(Sort.Direction.DESC, "id");
	}

	// ----------------------------------------------------------------------
	// PAGING
	// ----------------------------------------------------------------------

	/**
	 * Transforms criteria state into framework paging artifacts so pagination behavior stays predictable.
	 */
	public @Nullable Pageable toPageable() {

		Integer resolvedLimit = resolveLimit();
		if (resolvedLimit == null) {
			return null;
		}

		Integer resolvedOffset = resolveOffset(resolvedLimit);
		if (resolvedOffset == null) {
			return null;
		}

		return new OffsetBasedPageable(
				resolvedOffset,
				resolvedLimit,
				resolveSort()
		);
	}

	protected Integer resolveLimit() {

		if (limit == null || limit <= 0) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(limit, DEFAULT_MAX_ROWS);
	}

	protected Integer resolveOffset(Integer resolvedLimit) {

		// Offset has priority
		if (offset != null && offset >= 0) {
			return offset;
		}

		if (pageNumber != null && pageNumber > 0) {
			return (pageNumber - 1) * resolvedLimit;
		}

		return null;
	}

	/**
	 * Defines the criteria predicate contract so repositories apply the same business filters deterministically.
	 */
	public abstract Predicate getFilter(A root);

	// ----------------------------------------------------------------------
	// QUERYDSL CONTRACT
	// ----------------------------------------------------------------------

	/**
	 * Defines the criteria predicate contract so repositories apply the same business filters deterministically.
	 */
	public abstract Predicate getFilter();

	/**
	 * Exposes the target domain type so generic persistence flows can bind this criteria to the correct aggregate.
	 */
	public abstract Class<?> getObjectClass();

	private static final class OffsetBasedPageable implements Pageable, Serializable {

		@Serial
		private static final long serialVersionUID = 1L;

		private final long offset;
		private final int limit;
		private final Sort sort;

		private OffsetBasedPageable(long offset, int limit, Sort sort) {
			if (offset < 0) {
				throw new IllegalArgumentException("offset must be >= 0");
			}
			if (limit <= 0) {
				throw new IllegalArgumentException("limit must be > 0");
			}

			this.offset = offset;
			this.limit = limit;
			this.sort = (sort == null) ? Sort.unsorted() : sort;
		}

		/**
		 * Centralizes this criteria contract so query filtering and paging behavior remain consistent across repositories.
		 */
		@Override
		public int getPageNumber() {
			return (int) (offset / limit);
		}

		/**
		 * Centralizes this criteria contract so query filtering and paging behavior remain consistent across repositories.
		 */
		@Override
		public int getPageSize() {
			return limit;
		}

		/**
		 * Centralizes this criteria contract so query filtering and paging behavior remain consistent across repositories.
		 */
		@Override
		public long getOffset() {
			return offset;
		}

		/**
		 * Centralizes this criteria contract so query filtering and paging behavior remain consistent across repositories.
		 */
		@Override
		public Sort getSort() {
			return sort;
		}

		/**
		 * Centralizes this criteria contract so query filtering and paging behavior remain consistent across repositories.
		 */
		@Override
		public Pageable next() {
			return new OffsetBasedPageable(offset + limit, limit, sort);
		}

		/**
		 * Centralizes this criteria contract so query filtering and paging behavior remain consistent across repositories.
		 */
		@Override
		public Pageable previousOrFirst() {
			return hasPrevious()
					? new OffsetBasedPageable(Math.max(0, offset - limit), limit, sort)
					: first();
		}

		/**
		 * Centralizes this criteria contract so query filtering and paging behavior remain consistent across repositories.
		 */
		@Override
		public Pageable first() {
			return new OffsetBasedPageable(0, limit, sort);
		}

		/**
		 * Centralizes this criteria contract so query filtering and paging behavior remain consistent across repositories.
		 */
		@Override
		public Pageable withPage(int pageNumber) {
			if (pageNumber < 0) {
				throw new IllegalArgumentException("pageNumber must be >= 0");
			}
			return new OffsetBasedPageable((long) pageNumber * limit, limit, sort);
		}

		/**
		 * Centralizes this criteria contract so query filtering and paging behavior remain consistent across repositories.
		 */
		@Override
		public boolean hasPrevious() {
			return offset > 0;
		}
	}
}
