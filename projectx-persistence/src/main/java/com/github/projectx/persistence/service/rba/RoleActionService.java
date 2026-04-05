package com.github.projectx.persistence.service.rba;

import com.github.projectx.persistence.criteria.rba.RoleActionCriteria;
import com.github.projectx.persistence.dto.rba.RoleActionDTO;
import com.github.projectx.persistence.entity.rba.QRoleAction;
import com.github.projectx.persistence.entity.rba.RoleAction;
import com.github.projectx.persistence.mapper.rba.RoleActionMapper;
import com.github.projectx.persistence.repository.rba.RoleActionRepository;
import com.github.projectx.persistence.service.base.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class RoleActionService
		extends BaseService<
		Long,
		RoleAction,
		QRoleAction,
		RoleActionCriteria,
		RoleActionDTO,
		RoleActionMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + RoleActionService.class.getSimpleName());

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 *
	 * @param roleActionRepository input required by this operation contract
	 * @param mapper               input required by this operation contract
	 */
	public RoleActionService(
			RoleActionRepository roleActionRepository,
			RoleActionMapper mapper
	) {
		super(roleActionRepository, mapper);
	}
}

