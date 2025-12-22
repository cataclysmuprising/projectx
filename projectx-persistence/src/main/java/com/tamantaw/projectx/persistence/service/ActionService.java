package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.QAction;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.mapper.ActionMapper;
import com.tamantaw.projectx.persistence.repository.ActionRepository;
import com.tamantaw.projectx.persistence.service.base.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;

import static com.tamantaw.projectx.persistence.utils.LoggerConstants.LOG_PREFIX;
import static com.tamantaw.projectx.persistence.utils.LoggerConstants.LOG_SUFFIX;

@Service
public class ActionService
		extends BaseService<
		Action,
		QAction,
		ActionCriteria,
		ActionDTO,
		ActionMapper> {

	private static final Logger serviceLogger = LogManager.getLogger("serviceLogs." + ActionService.class);

	private final ActionRepository actionRepository;

	@Autowired
	public ActionService(ActionRepository actionRepository, ActionMapper mapper) {
		super(actionRepository, mapper);
		this.actionRepository = actionRepository;
	}

	@Transactional(readOnly = true)
	public List<String> selectPages(String appName) throws PersistenceException {
		Assert.notNull(appName, "App Name shouldn't be Null.");
		serviceLogger.info(LOG_PREFIX + "Transaction start for fetching role names by given appName : <{}>" + LOG_SUFFIX, appName);
		List<String> actionNames;
		try {
			actionNames = actionRepository.selectPages(appName);
		}
		catch (Exception e) {
			throw new PersistenceException(e.getMessage(), e);
		}
		serviceLogger.info(LOG_PREFIX + "Transaction finished successfully for fetching role names by given appName : <{}>" + LOG_SUFFIX, appName);
		return actionNames;
	}
}
