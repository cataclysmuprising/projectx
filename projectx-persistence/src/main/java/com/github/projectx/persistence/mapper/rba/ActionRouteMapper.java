package com.github.projectx.persistence.mapper.rba;

import com.github.projectx.persistence.config.MapStructConfig;
import com.github.projectx.persistence.dto.rba.ActionRouteDTO;
import com.github.projectx.persistence.entity.rba.ActionRoute;
import com.github.projectx.persistence.entity.rba.ActionRouteTarget;
import com.github.projectx.persistence.mapper.base.AbstractMapper;
import com.github.projectx.persistence.mapper.base.MappingContext;
import org.hibernate.Hibernate;
import org.mapstruct.*;

import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(config = MapStructConfig.class)
public abstract class ActionRouteMapper
		implements AbstractMapper<ActionRouteDTO, ActionRoute> {

	@Override
	@Mapping(target = "routeTargets", ignore = true)
	public abstract ActionRoute toEntity(ActionRouteDTO dto);

	@Override
	@Mapping(target = "actions", ignore = true)
	@Mapping(target = "actionIds", ignore = true)
	public abstract ActionRouteDTO toDto(
			ActionRoute entity,
			@Context MappingContext ctx
	);

	@AfterMapping
	protected void fillRelations(
			ActionRoute entity,
			@MappingTarget ActionRouteDTO dto,
			@Context MappingContext ctx
	) {
		if (entity == null || ctx == null || !ctx.isIncludeRelations()) {
			return;
		}

		var routeTargets = entity.getRouteTargets();
		if (routeTargets == null || !Hibernate.isInitialized(routeTargets)) {
			return;
		}

		dto.setActions(
				routeTargets.stream()
						.map(ActionRouteTarget::getAction)
						.filter(Objects::nonNull)
						.filter(Hibernate::isInitialized)
						.map(action -> ctx.getActionMapper().toDto(action, ctx))
						.collect(Collectors.toSet())
		);
	}
}
