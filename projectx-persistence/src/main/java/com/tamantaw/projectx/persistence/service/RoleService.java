package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.entity.QRole;
import com.tamantaw.projectx.persistence.entity.Role;
import com.tamantaw.projectx.persistence.mapper.RoleMapper;
import com.tamantaw.projectx.persistence.repository.RoleRepository;
import com.tamantaw.projectx.persistence.service.base.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleService
		extends BaseService<
		Role,
		QRole,
		RoleCriteria,
		RoleDTO,
		RoleMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + RoleService.class.getSimpleName());

	@Autowired
	public RoleService(RoleRepository roleRepository, RoleMapper mapper) {

		super(roleRepository, mapper);
	}
}

