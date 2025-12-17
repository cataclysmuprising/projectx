package com.tamantaw.projectx.persistence.mapper;

import com.tamantaw.projectx.persistence.config.MapStructConfig;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.AdministratorRole;
import com.tamantaw.projectx.persistence.mapper.base.AbstractMapper;
import com.tamantaw.projectx.persistence.mapper.base.MappingContext;
import org.hibernate.Hibernate;
import org.mapstruct.*;

import java.util.Objects;

@Mapper(
		config = MapStructConfig.class
)
public interface AdministratorMapper
		extends AbstractMapper<AdministratorDTO, Administrator> {

	// ----------------------------------------------------------------------
	// DTO → ENTITY (WRITE PATH)
	// ----------------------------------------------------------------------

	@Override
	@Mapping(target = "administratorRoles", ignore = true)
	Administrator toEntity(AdministratorDTO dto);

	// ----------------------------------------------------------------------
	// ENTITY → DTO (BASE MAPPING)
	// ----------------------------------------------------------------------

	@Override
	@Mapping(target = "roles", ignore = true)
	AdministratorDTO toDto(Administrator entity, @Context MappingContext ctx);

	// ----------------------------------------------------------------------
	// CONDITIONAL RELATION MAPPING (FETCH-AWARE, NO N+1)
	// ----------------------------------------------------------------------

	@AfterMapping
	default void fillRelations(
			Administrator entity,
			@MappingTarget AdministratorDTO dto,
			@Context MappingContext ctx
	) {
		if (entity == null || ctx == null) {
			return;
		}

		// ---------------- ROLES ----------------
		if (entity.getAdministratorRoles() != null
				&& Hibernate.isInitialized(entity.getAdministratorRoles())) {

			dto.setRoles(
					entity.getAdministratorRoles().stream()
							// unwrap join entity
							.map(AdministratorRole::getRole)
							.filter(Objects::nonNull)
							// avoid lazy initialization leaks
							.filter(Hibernate::isInitialized)
							// context-aware mapping (lambda, not method ref)
							.map(role -> ctx.getRoleMapper().toDto(role, ctx))
							.distinct()
							.toList()
			);
		}
	}
}
