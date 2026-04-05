package com.github.projectx.persistence.mapper.rba;

import com.github.projectx.persistence.config.MapStructConfig;
import com.github.projectx.persistence.dto.rba.AdministratorDTO;
import com.github.projectx.persistence.entity.rba.Administrator;
import com.github.projectx.persistence.entity.rba.AdministratorRole;
import com.github.projectx.persistence.mapper.base.AbstractMapper;
import com.github.projectx.persistence.mapper.base.MappingContext;
import org.hibernate.Hibernate;
import org.mapstruct.*;

import java.util.Objects;
import java.util.stream.Collectors;

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
		if (entity == null || ctx == null || !ctx.isIncludeRelations()) {
			return;
		}

		// ---------------- ROLES ----------------
		var adminRoles = entity.getAdministratorRoles();
		if (adminRoles != null && Hibernate.isInitialized(adminRoles)) {

			dto.setRoles(
					adminRoles.stream()
							// unwrap join entity
							.map(AdministratorRole::getRole)
							.filter(Objects::nonNull)
							// avoid lazy initialization leaks
							.filter(Hibernate::isInitialized)
							// context-aware mapping (lambda, not method ref)
							.map(role -> ctx.getRoleMapper().toDto(role, ctx))
							.collect(Collectors.toSet())
			);
		}
	}
}

