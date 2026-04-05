package com.github.projectx.persistence.repository.base;

import com.github.projectx.persistence.entity.base.AbstractEntity;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.jpa.impl.JPAUpdateClause;

/**
 * Type-safe bulk update specification for QueryDSL.
 * Use lambdas that reference generated Q-paths.
 */
@FunctionalInterface
public interface UpdateSpec<T extends AbstractEntity> {

	void apply(JPAUpdateClause update, EntityPathBase<T> root);
}
