package com.github.projectx.persistence.mapper.rba;

import com.github.projectx.persistence.config.MapStructConfig;
import com.github.projectx.persistence.dto.rba.AdministratorRoleDTO;
import com.github.projectx.persistence.entity.rba.AdministratorRole;
import com.github.projectx.persistence.mapper.base.AbstractMapper;
import com.github.projectx.persistence.mapper.base.MappingContext;
import org.hibernate.Hibernate;
import org.mapstruct.*;

@Mapper(
		config = MapStructConfig.class
)
public interface AdministratorRoleMapper
		extends AbstractMapper<AdministratorRoleDTO, AdministratorRole> {

	// ----------------------------------------------------------------------
	// DTO → ENTITY (WRITE PATH)
	// ----------------------------------------------------------------------

	@Override
	@Mapping(target = "administrator", ignore = true)
	@Mapping(target = "role", ignore = true)
	AdministratorRole toEntity(AdministratorRoleDTO dto);

	// ----------------------------------------------------------------------
	// ENTITY → DTO (BASE MAPPING)
	// ----------------------------------------------------------------------

	@Override
	@Mapping(target = "administrator", ignore = true)
	@Mapping(target = "role", ignore = true)
	AdministratorRoleDTO toDto(
			AdministratorRole entity,
			@Context MappingContext ctx
	);

	// ----------------------------------------------------------------------
	// CONDITIONAL RELATION MAPPING (FETCH-AWARE, NO N+1)
	// ----------------------------------------------------------------------

	@AfterMapping
	default void fillRelations(
			AdministratorRole entity,
			@MappingTarget AdministratorRoleDTO dto,
			@Context MappingContext ctx
	) {
		if (entity == null || ctx == null || !ctx.isIncludeRelations()) {
			return;
		}

		// ---------------- ADMINISTRATOR (to-one) ----------------
		var administrator = entity.getAdministrator();
		if (administrator != null && Hibernate.isInitialized(administrator)) {
			dto.setAdministrator(
					ctx.getAdministratorMapper()
							.toDto(administrator, ctx)
			);
		}

		// ---------------- ROLE (to-one) ----------------
		var role = entity.getRole();
		if (role != null && Hibernate.isInitialized(role)) {
			dto.setRole(
					ctx.getRoleMapper()
							.toDto(role, ctx)
			);
		}
	}
}

