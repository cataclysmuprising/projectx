package com.github.projectx.persistence.mapper.rba;

import com.github.projectx.persistence.config.MapStructConfig;
import com.github.projectx.persistence.dto.rba.RoleActionDTO;
import com.github.projectx.persistence.entity.rba.RoleAction;
import com.github.projectx.persistence.mapper.base.AbstractMapper;
import com.github.projectx.persistence.mapper.base.MappingContext;
import org.hibernate.Hibernate;
import org.mapstruct.*;

@Mapper(
		config = MapStructConfig.class
)
public interface RoleActionMapper
		extends AbstractMapper<RoleActionDTO, RoleAction> {

	// ----------------------------------------------------------------------
	// DTO → ENTITY (WRITE PATH)
	// ----------------------------------------------------------------------

	@Override
	@Mapping(target = "role", ignore = true)
	@Mapping(target = "action", ignore = true)
	RoleAction toEntity(RoleActionDTO dto);

	// ----------------------------------------------------------------------
	// ENTITY → DTO (BASE MAPPING)
	// ----------------------------------------------------------------------

	@Override
	@Mapping(target = "role", ignore = true)
	@Mapping(target = "action", ignore = true)
	RoleActionDTO toDto(
			RoleAction entity,
			@Context MappingContext ctx
	);

	// ----------------------------------------------------------------------
	// CONDITIONAL RELATION MAPPING (FETCH-AWARE, NO N+1)
	// ----------------------------------------------------------------------

	@AfterMapping
	default void fillRelations(
			RoleAction entity,
			@MappingTarget RoleActionDTO dto,
			@Context MappingContext ctx
	) {
		if (entity == null || ctx == null || !ctx.isIncludeRelations()) {
			return;
		}

		// ---------------- ROLE (to-one) ----------------
		var role = entity.getRole();
		if (role != null && Hibernate.isInitialized(role)) {
			dto.setRole(
					ctx.getRoleMapper()
							.toDto(role, ctx)
			);
		}

		// ---------------- ACTION (to-one) ----------------
		var action = entity.getAction();
		if (action != null && Hibernate.isInitialized(action)) {
			dto.setAction(
					ctx.getActionMapper()
							.toDto(action, ctx)
			);
		}
	}
}

