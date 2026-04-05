package com.github.projectx.backend.controller.rest.web.request;

import lombok.Data;

/**
 * Generic web API request contract for cursor/keyset search.
 *
 * <p>
 * Cursor contract:
 * <ul>
 *   <li>{@code afterIdExclusive == null}: first page</li>
 *   <li>{@code afterIdExclusive != null}: next page after this ID</li>
 * </ul>
 * </p>
 */
@Data
public class KeysetSearchRequest<C> {

	/**
	 * Domain-specific filter object (same object used by /search/paging).
	 */
	private C criteria;

	/**
	 * Last seen root ID from previous page.
	 * Null means fetch from beginning.
	 */
	private Long afterIdExclusive;

	/**
	 * Optional page size override.
	 * If null, service/repository default is used.
	 */
	private Integer limit;
}
