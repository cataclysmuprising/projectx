package com.tamantaw.projectx.persistence.mapper.base;

import org.mapstruct.Context;

import java.util.ArrayList;
import java.util.List;

public interface AbstractMapper<DTO, ENTITY> {

	ENTITY toEntity(DTO dto);

	/**
	 * Context-aware DTO mapping.
	 * Required for relational / fetch-aware mapping.
	 */
	DTO toDto(ENTITY entity, @Context MappingContext ctx);

	// ----------------------------------------------------------------------
	// COLLECTION HELPERS
	// ----------------------------------------------------------------------

	default List<DTO> mapToDtoList(
			Iterable<ENTITY> entityList,
			@Context MappingContext ctx
	) {
		if (entityList == null) {
			return List.of();
		}
		List<DTO> result = new ArrayList<>();
		for (ENTITY entity : entityList) {
			result.add(toDto(entity, ctx));
		}
		return result;
	}

	default List<ENTITY> mapToEntityList(Iterable<DTO> dtoList) {
		if (dtoList == null) {
			return List.of();
		}
		List<ENTITY> result = new ArrayList<>();
		for (DTO dto : dtoList) {
			result.add(toEntity(dto));
		}
		return result;
	}
}
