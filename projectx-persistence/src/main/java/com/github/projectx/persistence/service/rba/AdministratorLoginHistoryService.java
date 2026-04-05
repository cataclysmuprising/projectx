package com.github.projectx.persistence.service.rba;

import com.github.projectx.persistence.criteria.rba.AdministratorLoginHistoryCriteria;
import com.github.projectx.persistence.dto.rba.AdministratorLoginHistoryDTO;
import com.github.projectx.persistence.entity.rba.AdministratorLoginHistory;
import com.github.projectx.persistence.entity.rba.QAdministratorLoginHistory;
import com.github.projectx.persistence.mapper.rba.AdministratorLoginHistoryMapper;
import com.github.projectx.persistence.repository.rba.AdministratorLoginHistoryRepository;
import com.github.projectx.persistence.service.base.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class AdministratorLoginHistoryService
		extends BaseService<
		Long,
		AdministratorLoginHistory,
		QAdministratorLoginHistory,
		AdministratorLoginHistoryCriteria,
		AdministratorLoginHistoryDTO,
		AdministratorLoginHistoryMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + AdministratorLoginHistoryService.class.getSimpleName());

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 *
	 * @param administratorLoginHistoryRepository input required by this operation contract
	 * @param mapper                              input required by this operation contract
	 */
	public AdministratorLoginHistoryService(
			AdministratorLoginHistoryRepository administratorLoginHistoryRepository,
			AdministratorLoginHistoryMapper mapper
	) {
		super(administratorLoginHistoryRepository, mapper);
	}
}

