package com.github.projectx.persistence.repository.base;

import com.github.projectx.persistence.criteria.base.AbstractCriteria;
import com.github.projectx.persistence.entity.base.AbstractEntity;
import com.querydsl.core.types.dsl.EntityPathBase;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface AbstractRepository<
		ID extends Serializable & Comparable<ID>,
		ENTITY extends AbstractEntity,
		QCLAZZ extends EntityPathBase<ENTITY>,
		CRITERIA extends AbstractCriteria<QCLAZZ>
		> {

	// ------------------------------
	// ENTITY READS (internal/domain)
	// ------------------------------
	Optional<ENTITY> findById(ID id);

	Optional<ENTITY> findOne(CRITERIA criteria, String @Nullable ... hints);

	List<ENTITY> findAll(CRITERIA criteria, String @Nullable ... hints);

	/**
	 * Offset/limit pagination with strict deterministic guarantees.
	 *
	 * <p>
	 * This path returns total count metadata and therefore executes a count query.
	 * Use this when callers need page numbers or total pages.
	 * </p>
	 */
	Page<ENTITY> findByPaging(CRITERIA criteria, String @Nullable ... hints);

	/**
	 * Keyset/cursor pagination by primary key.
	 *
	 * <p>
	 * This path intentionally does not compute total count, which keeps it stable
	 * and cheap for deep-page traversal.
	 * Ordering is by root primary key only for deterministic cursor progression.
	 * Cursor semantics are:
	 * <ul>
	 *   <li>{@code afterIdExclusive == null}: start from the first page</li>
	 *   <li>{@code afterIdExclusive != null}: continue after that root ID</li>
	 * </ul>
	 * </p>
	 */
	KeysetPage<ENTITY, ID> findByKeyset(
			CRITERIA criteria,
			@Nullable ID afterIdExclusive,
			@Nullable Integer limit,
			String @Nullable ... hints
	);

	// ------------------------------
	// ID-BASED READS (safe)
	// ------------------------------
	List<ID> findIds(CRITERIA criteria);

	long count(CRITERIA criteria);

	boolean existsById(ID id);

	boolean exists(CRITERIA criteria);

	// ------------------------------
	// WRITE
	// ------------------------------
	ENTITY saveRecord(ENTITY entity);

	List<ENTITY> saveAllRecords(Iterable<ENTITY> entities);

	long updateById(UpdateSpec<ENTITY> spec, ID id, long updatedBy);

	<E extends ENTITY> long updateByCriteria(UpdateSpec<E> spec, CRITERIA criteria, Long updatedBy);

	/**
	 * Deletes a single entity by ID.
	 *
	 * @return true if deleted, false if not found
	 */
	boolean deleteWithId(ID id);

	long deleteByCriteria(CRITERIA criteria);
}

