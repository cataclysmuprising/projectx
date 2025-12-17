package com.tamantaw.projectx.persistence.mapper;

import com.tamantaw.projectx.persistence.config.MapStructConfig;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.mapper.base.AbstractMapper;
import org.mapstruct.Mapper;

@Mapper(config = MapStructConfig.class)
public abstract class ActionMapper implements AbstractMapper<ActionDTO, Action> {
}
