package com.github.projectx.persistence.mapper.rba;

import com.github.projectx.persistence.config.MapStructConfig;
import com.github.projectx.persistence.dto.rba.ActionDTO;
import com.github.projectx.persistence.entity.rba.Action;
import com.github.projectx.persistence.entity.rba.RoleAction;
import com.github.projectx.persistence.mapper.base.AbstractMapper;
import com.github.projectx.persistence.mapper.base.MappingContext;
import org.hibernate.Hibernate;
import org.mapstruct.*;

import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(config = MapStructConfig.class)
public abstract class ActionMapper
		implements AbstractMapper<ActionDTO, Action> {

	// ----------------------------------------------------------------------
	// DTO → ENTITY (WRITE PATH)
	// ----------------------------------------------------------------------

	/**
	 * Defines conversion behavior in one place so data mapping remains predictable between layers.
	 */
	@Override
	@Mapping(target = "roleActions", ignore = true)
	public abstract Action toEntity(ActionDTO dto);

	// ----------------------------------------------------------------------
	// ENTITY → DTO (BASE MAPPING)
	// ----------------------------------------------------------------------

	/**
	 * Keeps Action-to-DTO mapping centralized so relation expansion can be controlled by
	 * `MappingContext` and avoids accidental lazy-loading side effects.
	 */
	@Override
	@Mapping(target = "roles", ignore = true)
	public abstract ActionDTO toDto(
			Action entity,
			@Context MappingContext ctx
	);

	// ----------------------------------------------------------------------
	// CONDITIONAL RELATION MAPPING (FETCH-AWARE, NO N+1)
	// ----------------------------------------------------------------------

	@AfterMapping
	protected void fillRelations(
			Action entity,
			@MappingTarget ActionDTO dto,
			@Context MappingContext ctx
	) {
		if (entity == null || ctx == null || !ctx.isIncludeRelations()) {
			return;
		}

		// ---------------- ROLES ----------------
		var roleActions = entity.getRoleActions();
		if (roleActions != null && Hibernate.isInitialized(roleActions)) {

			dto.setRoles(
					roleActions.stream()
							// unwrap join entity
							.map(RoleAction::getRole)
							.filter(Objects::nonNull)
							// avoid lazy initialization leaks
							.filter(Hibernate::isInitialized)
							// context-aware mapping
							.map(role -> ctx.getRoleMapper().toDto(role, ctx))
							.collect(Collectors.toSet())
			);
		}
	}
}

