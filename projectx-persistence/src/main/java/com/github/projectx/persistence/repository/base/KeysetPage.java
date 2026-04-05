package com.github.projectx.persistence.repository.base;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Repository-internal immutable keyset/cursor page result.
 *
 * <p>
 * Why this type is separate from offset paging:
 * <ul>
 *   <li>No total-count semantics; keyset is optimized for forward scanning.</li>
 *   <li>Carries cursor continuation contract explicitly ({@code nextCursor}, {@code hasNext}).</li>
 *   <li>Keeps repository/service boundaries explicit before mapping to API DTOs.</li>
 * </ul>
 * </p>
 */
public final class KeysetPage<T, ID extends Serializable & Comparable<ID>> implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	@Getter
	private final List<T> data;
	@Getter
	private final @Nullable ID nextCursor;
	private final boolean hasNext;
	@Getter
	private final int pageSize;

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 *
	 * @param data       input required by this operation contract
	 * @param nextCursor input required by this operation contract
	 * @param hasNext    input required by this operation contract
	 * @param pageSize   input required by this operation contract
	 */
	public KeysetPage(
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
	 * Encapsulates deterministic data access for `hasNext` so callers do not duplicate query logic across services.
	 *
	 * @return result required by downstream orchestration logic
	 */
	public boolean hasNext() {
		return hasNext;
	}

	/**
	 * Encapsulates deterministic data access for `isEmpty` so callers do not duplicate query logic across services.
	 *
	 * @return result required by downstream orchestration logic
	 */
	public boolean isEmpty() {
		return data.isEmpty();
	}

	/**
	 * Encapsulates deterministic data access for `equals` so callers do not duplicate query logic across services.
	 *
	 * @param o input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof KeysetPage<?, ?> that)) {
			return false;
		}
		return hasNext == that.hasNext
				&& pageSize == that.pageSize
				&& Objects.equals(data, that.data)
				&& Objects.equals(nextCursor, that.nextCursor);
	}

	/**
	 * Encapsulates deterministic data access for `hashCode` so callers do not duplicate query logic across services.
	 *
	 * @return result required by downstream orchestration logic
	 */
	@Override
	public int hashCode() {
		return Objects.hash(data, nextCursor, hasNext, pageSize);
	}

	/**
	 * Encapsulates deterministic data access for `toString` so callers do not duplicate query logic across services.
	 *
	 * @return result required by downstream orchestration logic
	 */
	@Override
	public String toString() {
		return "KeysetPage{" +
				"dataSize=" + data.size() +
				", nextCursor=" + nextCursor +
				", hasNext=" + hasNext +
				", pageSize=" + pageSize +
				'}';
	}
}
