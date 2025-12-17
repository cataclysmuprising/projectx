package com.tamantaw.projectx.persistence.mapper;

import com.tamantaw.projectx.persistence.config.MapStructConfig;
import com.tamantaw.projectx.persistence.dto.RoleActionDTO;
import com.tamantaw.projectx.persistence.entity.RoleAction;
import com.tamantaw.projectx.persistence.mapper.base.AbstractMapper;
import com.tamantaw.projectx.persistence.mapper.base.MappingContext;
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
		if (entity == null || ctx == null) {
			return;
		}

		// ---------------- ROLE (to-one) ----------------
		if (entity.getRole() != null
				&& Hibernate.isInitialized(entity.getRole())) {

			dto.setRole(
					ctx.getRoleMapper()
							.toDto(entity.getRole(), ctx)
			);
		}

		// ---------------- ACTION (to-one) ----------------
		if (entity.getAction() != null
				&& Hibernate.isInitialized(entity.getAction())) {

			dto.setAction(
					ctx.getActionMapper()
							.toDto(entity.getAction(), ctx)
			);
		}
	}
}
