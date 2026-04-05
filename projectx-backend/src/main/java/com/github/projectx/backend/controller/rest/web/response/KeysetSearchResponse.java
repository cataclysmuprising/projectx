package com.github.projectx.backend.controller.rest.web.response;

import com.github.projectx.persistence.dto.base.KeysetResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Web API response contract for cursor/keyset search.
 *
 * <p>
 * This response intentionally does not include total-count metadata.
 * Clients should continue while {@code hasNext == true} using {@code nextCursor}.
 * </p>
 */
@Data
public class KeysetSearchResponse<T> {

	private List<T> data = new ArrayList<>();
	private Long nextCursor;
	private boolean hasNext;
	private int pageSize;

	public static <T> KeysetSearchResponse<T> from(KeysetResult<T, Long> result) {
		KeysetSearchResponse<T> response = new KeysetSearchResponse<>();
		if (result == null) {
			return response;
		}
		response.setData(result.getData());
		response.setNextCursor(result.getNextCursor());
		response.setHasNext(result.hasNext());
		response.setPageSize(result.getPageSize());
		return response;
	}
}
