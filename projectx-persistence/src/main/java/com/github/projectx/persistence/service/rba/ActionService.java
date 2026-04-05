package com.github.projectx.persistence.service.rba;

import com.github.projectx.persistence.criteria.rba.ActionCriteria;
import com.github.projectx.persistence.dto.rba.ActionDTO;
import com.github.projectx.persistence.entity.rba.Action;
import com.github.projectx.persistence.entity.rba.QAction;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.mapper.rba.ActionMapper;
import com.github.projectx.persistence.repository.rba.ActionRepository;
import com.github.projectx.persistence.service.base.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

import static com.github.projectx.persistence.utils.LoggerConstants.LOG_PREFIX;
import static com.github.projectx.persistence.utils.LoggerConstants.LOG_SUFFIX;

@Service
public class ActionService
		extends BaseService<
		Long,
		Action,
		QAction,
		ActionCriteria,
		ActionDTO,
		ActionMapper> {

	private static final Logger serviceLogger = LogManager.getLogger("serviceLogs." + ActionService.class);

	private final ActionRepository actionRepository;

	/**
	 * Wires required collaborators explicitly so repository data access stays deterministic and testable.
	 *
	 * @param actionRepository input required by this operation contract
	 * @param mapper           input required by this operation contract
	 */
	public ActionService(ActionRepository actionRepository, ActionMapper mapper) {
		super(actionRepository, mapper);
		this.actionRepository = actionRepository;
	}

	/**
	 * Encapsulates deterministic data access for `selectPages` so callers do not duplicate query logic across services.
	 *
	 * @param appName input required by this operation contract
	 * @return result required by downstream orchestration logic
	 */
	@Transactional(readOnly = true)
	public List<String> selectPages(String appName) throws PersistenceException {
		Assert.notNull(appName, "App Name shouldn't be Null.");
		serviceLogger.info(LOG_PREFIX + "Transaction start for fetching action pages by given appName : <{}>" + LOG_SUFFIX, appName);
		List<String> pages;
		try {
			pages = actionRepository.selectPages(appName);
		}
		catch (Exception e) {
			throw new PersistenceException(e.getMessage(), e);
		}
		serviceLogger.info(LOG_PREFIX + "Transaction finished successfully for fetching action pages by given appName : <{}>" + LOG_SUFFIX, appName);
		return pages;
	}
}

