package com.tamantaw.projectx.persistence.mapper.base;

import com.tamantaw.projectx.persistence.dto.base.AbstractDTO;
import com.tamantaw.projectx.persistence.dto.base.PaginatedResult;
import com.tamantaw.projectx.persistence.entity.base.AbstractEntity;
import org.springframework.data.domain.Page;

import java.util.List;

public class MapperUtils<DTO extends AbstractDTO> {
	public static <DTO extends AbstractDTO> PaginatedResult<DTO> toPaginatedResult(Page<? extends AbstractEntity> page, List<DTO> data) {
		return new PaginatedResult<>(page.getTotalElements(), page.getTotalElements(), page.getTotalPages(), page.getSize(), page.getNumber(), page.getNumberOfElements(), page.getSort(), data);
	}
}
