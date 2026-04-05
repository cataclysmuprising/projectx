package com.github.projectx.persistence.mapper.base;

import com.github.projectx.persistence.mapper.rba.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
public class MappingContext {

	private final ThreadLocal<Boolean> includeRelations =
			ThreadLocal.withInitial(() -> Boolean.TRUE);

	private final RoleMapper roleMapper;
	private final ActionMapper actionMapper;
	private final AdministratorMapper administratorMapper;
	private final AdministratorRoleMapper administratorRoleMapper;
	private final RoleActionMapper roleActionMapper;
	private final AdministratorLoginHistoryMapper administratorLoginHistoryMapper;

	/**
	 * Centralizes this decision contract so boolean checks stay consistent across call sites.
	 */
	public boolean isIncludeRelations() {
		return Boolean.TRUE.equals(includeRelations.get());
	}

	/**
	 * Documents state mutation intent so updates remain explicit and traceable in shared domain/config objects.
	 */
	public void setIncludeRelations(boolean include) {
		includeRelations.set(include);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public void clearIncludeRelations() {
		includeRelations.remove();
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public <T> T withIncludeRelations(boolean include, java.util.function.Supplier<T> supplier) {
		Boolean previous = includeRelations.get();
		includeRelations.set(include);
		try {
			return supplier.get();
		}
		finally {
			includeRelations.set(previous);
		}
	}
}

