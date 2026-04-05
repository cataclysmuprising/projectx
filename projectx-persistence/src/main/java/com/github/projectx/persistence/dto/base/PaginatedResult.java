package com.github.projectx.persistence.dto.base;

import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generic paginated result DTO.
 * <p>
 * - Cache-safe (Redis / JSON)
 * - No Spring / JPA types inside
 * - Immutable by design
 */
@Getter
public final class PaginatedResult<T> implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final long totalElements;
	private final long filteredElements;
	private final int totalPages;
	private final int pageSize;
	private final int pageNumber; // 0-based
	private final int numberOfElements;

	/**
	 * Sort definition used for this page.
	 * Empty list means "unsorted / default".
	 */
	private final List<SortItem> sort;

	private final List<T> data;

	/**
	 * Materializes page metadata and payload in one immutable transport object so API layers can
	 * serialize paging results without leaking Spring `Page` internals.
	 */
	public PaginatedResult(
			long totalElements,
			long filteredElements,
			int totalPages,
			int pageSize,
			int pageNumber,
			int numberOfElements,
			List<SortItem> sort,
			List<T> data
	) {
		this.totalElements = totalElements;
		this.filteredElements = filteredElements;
		this.totalPages = totalPages;
		this.pageSize = pageSize;
		this.pageNumber = pageNumber;
		this.numberOfElements = numberOfElements;
		this.sort = (sort == null) ? new ArrayList<>() : new ArrayList<>(sort);
		this.data = (data == null) ? new ArrayList<>() : new ArrayList<>(data);
	}

	// ---------------------------------------------------------------------
	// Getters
	// ---------------------------------------------------------------------

	// ---------------------------------------------------------------------
	// Convenience helpers
	// ---------------------------------------------------------------------

	/**
	 * Centralizes this decision contract so boolean checks stay consistent across call sites.
	 */
	public boolean isEmpty() {
		return data.isEmpty();
	}

	/**
	 * Centralizes this decision contract so boolean checks stay consistent across call sites.
	 */
	public boolean hasNext() {
		return totalPages > 0 && pageNumber >= 0 && pageNumber + 1 < totalPages;
	}

	/**
	 * Centralizes this decision contract so boolean checks stay consistent across call sites.
	 */
	public boolean hasPrevious() {
		return pageNumber > 0;
	}

	// ---------------------------------------------------------------------
	// equals / hashCode / toString
	// ---------------------------------------------------------------------

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof PaginatedResult<?> that)) {
			return false;
		}
		return totalElements == that.totalElements
				&& filteredElements == that.filteredElements
				&& totalPages == that.totalPages
				&& pageSize == that.pageSize
				&& pageNumber == that.pageNumber
				&& numberOfElements == that.numberOfElements
				&& Objects.equals(sort, that.sort)
				&& Objects.equals(data, that.data);
	}

	/**
	 * Centralizes this decision contract so boolean checks stay consistent across call sites.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(
				totalElements,
				filteredElements,
				totalPages,
				pageSize,
				pageNumber,
				numberOfElements,
				sort,
				data
		);
	}

	/**
	 * Defines conversion behavior in one place so data mapping remains predictable between layers.
	 */
	@Override
	public String toString() {
		return "PaginatedResult{" +
				"totalElements=" + totalElements +
				", filteredElements=" + filteredElements +
				", totalPages=" + totalPages +
				", pageSize=" + pageSize +
				", pageNumber=" + pageNumber +
				", numberOfElements=" + numberOfElements +
				", sort=" + sort +
				", dataSize=" + data.size() +
				'}';
	}
}
