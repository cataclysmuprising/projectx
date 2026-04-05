package com.github.projectx.persistence.mapper.rba;

import com.github.projectx.persistence.config.MapStructConfig;
import com.github.projectx.persistence.dto.rba.RoleDTO;
import com.github.projectx.persistence.entity.rba.AdministratorRole;
import com.github.projectx.persistence.entity.rba.Role;
import com.github.projectx.persistence.entity.rba.RoleAction;
import com.github.projectx.persistence.mapper.base.AbstractMapper;
import com.github.projectx.persistence.mapper.base.MappingContext;
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
		if (entity == null || ctx == null || !ctx.isIncludeRelations()) {
			return;
		}

		// ---------------- ADMINISTRATORS ----------------
		var adminRoles = entity.getAdministratorRoles();
		if (adminRoles != null && Hibernate.isInitialized(adminRoles)) {

			dto.setAdministrators(
					adminRoles.stream()
							.map(AdministratorRole::getAdministrator)
							.filter(Objects::nonNull)
							.filter(Hibernate::isInitialized)
							.map(admin -> ctx.getAdministratorMapper().toDto(admin, ctx))
							.collect(Collectors.toSet())
			);
		}

		// ---------------- ACTIONS ----------------
		var roleActions = entity.getRoleActions();
		if (roleActions != null && Hibernate.isInitialized(roleActions)) {

			dto.setActions(
					roleActions.stream()
							.map(RoleAction::getAction)
							.filter(Objects::nonNull)
							.filter(Hibernate::isInitialized)
							.map(action -> ctx.getActionMapper().toDto(action, ctx))
							.collect(Collectors.toSet())
			);
		}
	}
}

