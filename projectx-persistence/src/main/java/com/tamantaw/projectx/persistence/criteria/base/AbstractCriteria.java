package com.tamantaw.projectx.persistence.criteria.base;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.tamantaw.projectx.persistence.entity.base.QAbstractEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.*;

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

	// COMMON KEYWORD
	protected String keyword;

	// ----------------------------------------------------------------------
	// SORTING (MULTI-COLUMN)
	// ----------------------------------------------------------------------

	/**
	 * Ordered sort fields.
	 * Example:
	 * id -> DESC
	 * name -> ASC
	 */
	protected final LinkedHashMap<String, Sort.Direction> sortOrders = new LinkedHashMap<>();

	// ----------------------------------------------------------------------
	// PAGING INPUTS
	// ----------------------------------------------------------------------

	/**
	 * Page number (1-based, controller-friendly).
	 * If provided, offset is calculated automatically.
	 */
	protected Integer pageNumber;

	/**
	 * Zero-based offset (advanced usage).
	 */
	protected Integer offset;

	/**
	 * Page size / limit.
	 */
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
	// SORTING
	// ----------------------------------------------------------------------

	public void addSort(String property, Sort.Direction direction) {
		if (StringUtils.isNotBlank(property) && direction != null) {
			sortOrders.put(property, direction);
		}
	}

	public Sort resolveSort() {

		if (sortOrders.isEmpty()) {
			return Sort.by(Sort.Direction.DESC, "id");
		}

		List<Sort.Order> orders = new ArrayList<>();
		for (Map.Entry<String, Sort.Direction> e : sortOrders.entrySet()) {
			orders.add(new Sort.Order(e.getValue(), e.getKey()));
		}
		return Sort.by(orders);
	}

	// ----------------------------------------------------------------------
	// PAGING
	// ----------------------------------------------------------------------

	/**
	 * Computes {@link Pageable} dynamically based on controller inputs.
	 *
	 * <p><b>Supported controller input combinations</b>:
	 * <ul>
	 *   <li><b>pageNumber only</b> → offset is auto-calculated</li>
	 *   <li><b>pageNumber + limit</b> → offset is auto-calculated</li>
	 *   <li><b>offset + limit</b> → raw offset-based paging</li>
	 *   <li><b>nothing provided</b> → paging is NOT applied</li>
	 * </ul>
	 *
	 * <p><b>Important contract rule</b>:
	 * <ul>
	 *   <li>Paging is <b>opt-in</b>.</li>
	 *   <li>{@code limit} alone must NOT trigger paging.</li>
	 *   <li>{@code findAll()} must never silently truncate results.</li>
	 * </ul>
	 */
	public Pageable toPageable() {

		// ----------------------------------------------------------
		// Paging must be explicitly requested.
		// If neither pageNumber nor offset is provided,
		// this query is considered NON-PAGED.
		// ----------------------------------------------------------
		if (pageNumber == null && offset == null) {
			return null;
		}

		Integer resolvedLimit = resolveLimit();
		Integer resolvedOffset = resolveOffset(resolvedLimit);

		// Defensive guard: incomplete paging state
		if (resolvedLimit == null || resolvedOffset == null) {
			return null;
		}

		int pageIndex = resolvedOffset / resolvedLimit;
		return PageRequest.of(pageIndex, resolvedLimit, resolveSort());
	}

	/**
	 * Resolves the effective page size.
	 *
	 * <p>Rules:
	 * <ul>
	 *   <li>If limit is missing or invalid → use DEFAULT_PAGE_SIZE</li>
	 *   <li>Never exceed DEFAULT_MAX_ROWS</li>
	 * </ul>
	 */
	protected Integer resolveLimit() {

		if (limit == null || limit <= 0) {
			return DEFAULT_PAGE_SIZE;
		}

		return Math.min(limit, DEFAULT_MAX_ROWS);
	}

	/**
	 * Resolves the effective offset.
	 *
	 * <p>Resolution priority:
	 * <ol>
	 *   <li>pageNumber (1-based, controller-friendly)</li>
	 *   <li>explicit offset (advanced usage)</li>
	 * </ol>
	 *
	 * <p>Note:
	 * <ul>
	 *   <li>This method is only called when paging is explicitly requested.</li>
	 *   <li>{@code limit}-only is intentionally ignored to avoid silent paging.</li>
	 * </ul>
	 */
	protected Integer resolveOffset(Integer resolvedLimit) {

		// ----------------------------------------------------------
		// Page-based paging (1-based page index from controller)
		// ----------------------------------------------------------
		if (pageNumber != null && pageNumber > 0) {
			return (pageNumber - 1) * resolvedLimit;
		}

		// ----------------------------------------------------------
		// Raw offset-based paging (advanced / internal use)
		// ----------------------------------------------------------
		if (offset != null && offset >= 0) {
			return offset;
		}

		// ----------------------------------------------------------
		// No valid paging input → no paging
		// ----------------------------------------------------------
		return null;
	}

	// ----------------------------------------------------------------------
	// QUERYDSL CONTRACT
	// ----------------------------------------------------------------------

	/**
	 * Predicate with root provided (preferred).
	 */
	public abstract Predicate getFilter(A root);

	/**
	 * Predicate without root (used by repository base).
	 */
	public abstract Predicate getFilter();

	/**
	 * Domain class (for logging / diagnostics only).
	 */
	public abstract Class<?> getObjectClass();
}
