package com.github.projectx.persistence.dto.base;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * API-facing DTO wrapper for keyset/cursor paging.
 *
 * <p>
 * Why this exists:
 * <ul>
 *   <li>Keeps service contracts free from repository/internal types.</li>
 *   <li>Represents cursor pagination explicitly (next cursor + hasNext).</li>
 *   <li>Avoids expensive/unstable total-count computation for deep scans.</li>
 * </ul>
 * </p>
 */
@Getter
public final class KeysetResult<T, ID extends Serializable & Comparable<ID>> implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	private final List<T> data;
	private final @Nullable ID nextCursor;
	private final boolean hasNext;
	private final int pageSize;

	/**
	 * Freezes keyset page metadata at creation time so callers can traverse cursors deterministically
	 * without mutating repository output.
	 */
	public KeysetResult(
			List<T> data,
			@Nullable ID nextCursor,
			boolean hasNext,
			int pageSize) {

		if (pageSize <= 0) {
			throw new IllegalArgumentException("pageSize must be > 0");
		}

		this.data = new ArrayList<>(Objects.requireNonNull(data, "data must not be null"));
		this.nextCursor = nextCursor;
		this.hasNext = hasNext;
		this.pageSize = pageSize;
	}

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
		return hasNext;
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof KeysetResult<?, ?> that)) {
			return false;
		}
		return hasNext == that.hasNext
				&& pageSize == that.pageSize
				&& Objects.equals(data, that.data)
				&& Objects.equals(nextCursor, that.nextCursor);
	}

	/**
	 * Centralizes this decision contract so boolean checks stay consistent across call sites.
	 */
	@Override
	public int hashCode() {
		return Objects.hash(data, nextCursor, hasNext, pageSize);
	}

	/**
	 * Defines conversion behavior in one place so data mapping remains predictable between layers.
	 */
	@Override
	public String toString() {
		return "KeysetResult{" +
				"dataSize=" + data.size() +
				", nextCursor=" + nextCursor +
				", hasNext=" + hasNext +
				", pageSize=" + pageSize +
				'}';
	}
}
