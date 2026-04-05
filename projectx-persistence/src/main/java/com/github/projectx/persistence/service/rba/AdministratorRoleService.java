package com.github.projectx.persistence.service.rba;

import com.github.projectx.persistence.criteria.rba.AdministratorRoleCriteria;
import com.github.projectx.persistence.dto.rba.AdministratorRoleDTO;
import com.github.projectx.persistence.entity.rba.AdministratorRole;
import com.github.projectx.persistence.entity.rba.QAdministratorRole;
import com.github.projectx.persistence.mapper.rba.AdministratorRoleMapper;
import com.github.projectx.persistence.repository.rba.AdministratorRoleRepository;
import com.github.projectx.persistence.service.base.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class AdministratorRoleService
		extends BaseService<
		Long,
		AdministratorRole,
		QAdministratorRole,
		AdministratorRoleCriteria,
		AdministratorRoleDTO,
		AdministratorRoleMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + AdministratorRoleService.class.getSimpleName());

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 *
	 * @param administratorRoleRepository input required by this operation contract
	 * @param mapper                      input required by this operation contract
	 */
	public AdministratorRoleService(
			AdministratorRoleRepository administratorRoleRepository,
			AdministratorRoleMapper mapper
	) {
		super(administratorRoleRepository, mapper);
	}
}

