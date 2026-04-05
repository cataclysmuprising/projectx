package com.github.projectx.persistence.mapper.rba;

import com.github.projectx.persistence.config.MapStructConfig;
import com.github.projectx.persistence.dto.rba.AdministratorLoginHistoryDTO;
import com.github.projectx.persistence.entity.rba.AdministratorLoginHistory;
import com.github.projectx.persistence.mapper.base.AbstractMapper;
import com.github.projectx.persistence.mapper.base.MappingContext;
import org.hibernate.Hibernate;
import org.mapstruct.*;

@Mapper(
		config = MapStructConfig.class
)
public interface AdministratorLoginHistoryMapper
		extends AbstractMapper<AdministratorLoginHistoryDTO, AdministratorLoginHistory> {

	@Override
	@Mapping(target = "administrator", ignore = true)
	AdministratorLoginHistory toEntity(AdministratorLoginHistoryDTO dto);

	@Override
	@Mapping(target = "administrator", ignore = true)
	AdministratorLoginHistoryDTO toDto(
			AdministratorLoginHistory entity,
			@Context MappingContext ctx
	);

	@AfterMapping
	default void fillRelations(
			AdministratorLoginHistory entity,
			@MappingTarget AdministratorLoginHistoryDTO dto,
			@Context MappingContext ctx
	) {
		if (entity == null || ctx == null || !ctx.isIncludeRelations()) {
			return;
		}

		var administrator = entity.getAdministrator();
		if (administrator != null && Hibernate.isInitialized(administrator)) {
			dto.setAdministrator(
					ctx.getAdministratorMapper()
							.toDto(administrator, ctx)
			);
		}
	}
}

