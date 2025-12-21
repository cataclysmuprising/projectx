package com.tamantaw.projectx.persistence.mapper;

import com.tamantaw.projectx.persistence.config.MapStructConfig;
import com.tamantaw.projectx.persistence.dto.AdministratorLoginHistoryDTO;
import com.tamantaw.projectx.persistence.entity.AdministratorLoginHistory;
import com.tamantaw.projectx.persistence.mapper.base.AbstractMapper;
import com.tamantaw.projectx.persistence.mapper.base.MappingContext;
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
		if (entity == null || ctx == null) {
			return;
		}

		if (entity.getAdministrator() != null
				&& Hibernate.isInitialized(entity.getAdministrator())) {

			dto.setAdministrator(
					ctx.getAdministratorMapper()
							.toDto(entity.getAdministrator(), ctx)
			);
		}
	}
}
