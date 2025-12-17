package com.tamantaw.projectx.persistence.repository.base;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.tamantaw.projectx.persistence.criteria.base.AbstractCriteria;
import com.tamantaw.projectx.persistence.entity.base.AbstractEntity;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface AbstractRepository<
		ENTITY extends AbstractEntity,
		QCLAZZ extends EntityPathBase<ENTITY>,
		CRITERIA extends AbstractCriteria<QCLAZZ>,
		ID extends Serializable> {

	// ------------------------------
	// ENTITY READS (internal/domain)
	// ------------------------------
	Optional<ENTITY> findById(ID id);

	Optional<ENTITY> findOne(CRITERIA criteria, String... hints);

	List<ENTITY> findAll(CRITERIA criteria, String... hints);

	Page<ENTITY> findByPaging(CRITERIA criteria, String... hints);

	// ------------------------------
	// ID-BASED READS (safe)
	// ------------------------------
	List<Long> findIds(CRITERIA criteria);

	long count(CRITERIA criteria);

	boolean exists(CRITERIA criteria);

	// ------------------------------
	// WRITE
	// ------------------------------
	ENTITY saveRecord(ENTITY entity);

	List<ENTITY> saveAllRecords(Iterable<ENTITY> entities);

	<E extends ENTITY> long updateByCriteria(UpdateSpec<E> spec, CRITERIA criteria, Long updatedBy);

	long deleteByCriteria(CRITERIA criteria);
}



