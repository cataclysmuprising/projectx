package com.tamantaw.projectx.persistence.mapper;

import com.tamantaw.projectx.persistence.config.MapStructConfig;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.RoleAction;
import com.tamantaw.projectx.persistence.mapper.base.AbstractMapper;
import com.tamantaw.projectx.persistence.mapper.base.MappingContext;
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

	@Override
	@Mapping(target = "roleActions", ignore = true)
	public abstract Action toEntity(ActionDTO dto);

	// ----------------------------------------------------------------------
	// ENTITY → DTO (BASE MAPPING)
	// ----------------------------------------------------------------------

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
		if (entity == null || ctx == null) {
			return;
		}

		// ---------------- ROLES ----------------
		if (entity.getRoleActions() != null
				&& Hibernate.isInitialized(entity.getRoleActions())) {

			dto.setRoles(
					entity.getRoleActions().stream()
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
