package com.tamantaw.projectx.persistence.mapper;

import com.tamantaw.projectx.persistence.config.MapStructConfig;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.entity.AdministratorRole;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.entity.RoleAction;
import com.tamantaw.projectx.persistence.mapper.base.AbstractMapper;
import com.tamantaw.projectx.persistence.mapper.base.MappingContext;
import org.hibernate.Hibernate;
import org.mapstruct.*;

import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(
		config = MapStructConfig.class
)
public interface RoleMapper extends AbstractMapper<RoleDTO, Role> {

	// ----------------------------------------------------------------------
	// DTO → ENTITY (WRITE PATH)
	// ----------------------------------------------------------------------

	@Override
	@Mapping(target = "roleActions", ignore = true)
	@Mapping(target = "administratorRoles", ignore = true)
	Role toEntity(RoleDTO dto);

	// ----------------------------------------------------------------------
	// ENTITY → DTO (BASE MAPPING)
	// ----------------------------------------------------------------------

	@Override
	@Mapping(target = "administrators", ignore = true)
	@Mapping(target = "actions", ignore = true)
	RoleDTO toDto(Role entity, @Context MappingContext ctx);

	// ----------------------------------------------------------------------
	// CONDITIONAL RELATION MAPPING (FETCH-AWARE, NO N+1)
	// ----------------------------------------------------------------------

	@AfterMapping
	default void fillRelations(
			Role entity,
			@MappingTarget RoleDTO dto,
			@Context MappingContext ctx
	) {
		if (entity == null || ctx == null) {
			return;
		}

		// ---------------- ADMINISTRATORS ----------------
		if (entity.getAdministratorRoles() != null
				&& Hibernate.isInitialized(entity.getAdministratorRoles())) {

			dto.setAdministrators(
					entity.getAdministratorRoles().stream()
							.map(AdministratorRole::getAdministrator)
							.filter(Objects::nonNull)
							.filter(Hibernate::isInitialized)
							.map(admin -> ctx.getAdministratorMapper().toDto(admin, ctx))
							.collect(Collectors.toSet())
			);
		}

		// ---------------- ACTIONS ----------------
		if (entity.getRoleActions() != null
				&& Hibernate.isInitialized(entity.getRoleActions())) {

			dto.setActions(
					entity.getRoleActions().stream()
							.map(RoleAction::getAction)
							.filter(Objects::nonNull)
							.filter(Hibernate::isInitialized)
							.map(action -> ctx.getActionMapper().toDto(action, ctx))
							.distinct()
							.collect(Collectors.toSet())
			);
		}
	}
}
