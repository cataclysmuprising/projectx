package com.tamantaw.projectx.persistence.mapper;

import com.tamantaw.projectx.persistence.config.MapStructConfig;
import com.tamantaw.projectx.persistence.dto.AdministratorRoleDTO;
import com.tamantaw.projectx.persistence.entity.AdministratorRole;
import com.tamantaw.projectx.persistence.mapper.base.AbstractMapper;
import com.tamantaw.projectx.persistence.mapper.base.MappingContext;
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
		if (entity == null || ctx == null) {
			return;
		}

		// ---------------- ADMINISTRATOR (to-one) ----------------
		if (entity.getAdministrator() != null
				&& Hibernate.isInitialized(entity.getAdministrator())) {

			dto.setAdministrator(
					ctx.getAdministratorMapper()
							.toDto(entity.getAdministrator(), ctx)
			);
		}

		// ---------------- ROLE (to-one) ----------------
		if (entity.getRole() != null
				&& Hibernate.isInitialized(entity.getRole())) {

			dto.setRole(
					ctx.getRoleMapper()
							.toDto(entity.getRole(), ctx)
			);
		}
	}
}
