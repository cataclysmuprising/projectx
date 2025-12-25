package com.tamantaw.projectx.persistence.criteria.base;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.ComparableExpressionBase;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.tamantaw.projectx.persistence.entity.base.QAbstractEntity;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

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
	protected static final int DEFAULT_MAX_ROWS = 100;

	// ----------------------------------------------------------------------
	// COMMON FILTER FIELDS
	// ----------------------------------------------------------------------
	protected final List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();
	protected Long id;
	protected Long fromId;
	protected Set<Long> includeIds;
	protected Set<Long> excludeIds;
	protected Long createdBy;
	protected Long updatedBy;
	protected LocalDateTime createdDateFrom;
	protected LocalDateTime createdDateTo;
	protected LocalDateTime updatedDateFrom;
	protected LocalDateTime updatedDateTo;

	// ----------------------------------------------------------------------
	// SORTING (JAVA SIDE – TYPE SAFE)
	// ----------------------------------------------------------------------
	protected String keyword;

	// ----------------------------------------------------------------------
	// SORTING (CLIENT SIDE – STRING BASED)
	// ----------------------------------------------------------------------
	/**
	 * Client-provided sort keys (column names).
	 * Order matters.
	 */
	protected List<String> sortKeys;

	/**
	 * Client-provided sort directions.
	 * Must align with sortKeys by index.
	 */
	protected List<Sort.Direction> sortDirs;

	// ----------------------------------------------------------------------
	// PAGING INPUTS
	// ----------------------------------------------------------------------

	protected Integer pageNumber;
	protected Integer offset;
	protected Integer limit;

	// ----------------------------------------------------------------------
	// COMMON FILTER LOGIC
	// ----------------------------------------------------------------------

	protected BooleanBuilder commonFilter(QAbstractEntity audit) {

		BooleanBuilder predicate = new BooleanBuilder();

		if (id != null) {
			predicate.and(audit.id.eq(id));
		}
		if (fromId != null) {
			predicate.and(audit.id.gt(fromId));
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
	// SORTING – JAVA SIDE
	// ----------------------------------------------------------------------

	public void addSort(
			ComparableExpressionBase<? extends Comparable<?>> property,
			Sort.Direction direction) {

		if (property == null || direction == null) {
			return;
		}

		Order order = direction.isAscending() ? Order.ASC : Order.DESC;
		orderSpecifiers.add(new OrderSpecifier<>(order, property));
	}

	// ----------------------------------------------------------------------
	// SORTING – RESOLUTION (JAVA + CLIENT)
	// ----------------------------------------------------------------------

	public List<OrderSpecifier<?>> resolveOrderSpecifiers(EntityPathBase<?> root) {

		// 1️⃣ Explicit Java-side sorting (highest priority)
		if (!CollectionUtils.isEmpty(orderSpecifiers)) {
			return List.copyOf(orderSpecifiers);
		}

		// 2️⃣ Client-side string sorting (multi-column)
		if (root != null
				&& !CollectionUtils.isEmpty(sortKeys)
				&& !CollectionUtils.isEmpty(sortDirs)
				&& sortKeys.size() == sortDirs.size()) {

			PathBuilder<?> pb =
					new PathBuilder<>(root.getType(), root.getMetadata());

			List<OrderSpecifier<?>> clientOrders = new ArrayList<>();

			for (int i = 0; i < sortKeys.size(); i++) {
				String key = sortKeys.get(i);
				Sort.Direction dir = sortDirs.get(i);

				ComparableExpressionBase<?> expr =
						pb.getComparable(key, Comparable.class); // may throw → accepted

				Order order = dir.isAscending() ? Order.ASC : Order.DESC;
				clientOrders.add(new OrderSpecifier<>(order, expr));
			}

			if (!clientOrders.isEmpty()) {
				return clientOrders;
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

	// ----------------------------------------------------------------------
	// SPRING DATA SORT (FOR Pageable)
	// ----------------------------------------------------------------------

	public Sort resolveSort() {

		List<OrderSpecifier<?>> orders = resolveOrderSpecifiers(null);

		if (CollectionUtils.isEmpty(orders)) {
			return Sort.by(Sort.Direction.DESC, "id");
		}

		List<Sort.Order> sortOrders = new ArrayList<>();

		for (OrderSpecifier<?> o : orders) {
			Sort.Direction direction =
					o.getOrder() == Order.ASC
							? Sort.Direction.ASC
							: Sort.Direction.DESC;

			String propertyName = resolvePropertyName(o);
			if (propertyName == null) {
				throw new IllegalArgumentException(
						"Unable to resolve property name from order specifier: " + o
				);
			}

			sortOrders.add(new Sort.Order(direction, propertyName));
		}

		return Sort.by(sortOrders);
	}

	private String resolvePropertyName(OrderSpecifier<?> orderSpecifier) {

		if (orderSpecifier == null || orderSpecifier.getTarget() == null) {
			return null;
		}

		if (orderSpecifier.getTarget() instanceof Path<?> path) {
			return path.getMetadata().getName();
		}

		return null;
	}

	// ----------------------------------------------------------------------
// SORTING – STRING BASED (CLIENT OR JAVA)
// ----------------------------------------------------------------------

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

	public void clearStringSorts() {
		if (sortKeys != null) {
			sortKeys.clear();
		}
		if (sortDirs != null) {
			sortDirs.clear();
		}
	}

	// ----------------------------------------------------------------------
	// PAGING
	// ----------------------------------------------------------------------

	public Pageable toPageable() {

		if (pageNumber == null && offset == null) {
			return null;
		}

		Integer resolvedLimit = resolveLimit();
		Integer resolvedOffset = resolveOffset(resolvedLimit);

		if (resolvedLimit == null || resolvedOffset == null) {
			return null;
		}

		int pageIndex = resolvedOffset / resolvedLimit;
		return PageRequest.of(pageIndex, resolvedLimit, resolveSort());
	}

	protected Integer resolveLimit() {

		if (limit == null || limit <= 0) {
			return DEFAULT_PAGE_SIZE;
		}

		return Math.min(limit, DEFAULT_MAX_ROWS);
	}

	protected Integer resolveOffset(Integer resolvedLimit) {

		if (pageNumber != null && pageNumber > 0) {
			return (pageNumber - 1) * resolvedLimit;
		}

		if (offset != null && offset >= 0) {
			return offset;
		}

		return null;
	}

	// ----------------------------------------------------------------------
	// QUERYDSL CONTRACT
	// ----------------------------------------------------------------------

	public abstract Predicate getFilter(A root);

	public abstract Predicate getFilter();

	public abstract Class<?> getObjectClass();
}
