package com.tamantaw.projectx.persistence.repository.base;

import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAUpdateClause;
import com.tamantaw.projectx.persistence.entity.base.AbstractEntity;

/**
 * Type-safe bulk update specification for QueryDSL.
 * Use lambdas that reference generated Q-paths.
 */
@FunctionalInterface
public interface UpdateSpec<T extends AbstractEntity> {

	void apply(JPAUpdateClause update, EntityPathBase<T> root);
}