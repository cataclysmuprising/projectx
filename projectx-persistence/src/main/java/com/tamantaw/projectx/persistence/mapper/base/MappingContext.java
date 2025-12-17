package com.tamantaw.projectx.persistence.mapper.base;

import com.tamantaw.projectx.persistence.mapper.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@Getter
@RequiredArgsConstructor
public class MappingContext {

	private final RoleMapper roleMapper;
	private final ActionMapper actionMapper;
	private final AdministratorMapper administratorMapper;
	private final AdministratorRoleMapper administratorRoleMapper;
	private final RoleActionMapper roleActionMapper;
}


