package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.QAction;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.mapper.ActionMapper;
import com.tamantaw.projectx.persistence.repository.ActionRepository;
import com.tamantaw.projectx.persistence.service.base.BaseService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

	@Transactional(readOnly = true)
	public List<String> selectAvailableActionsForUser(String page, String appName, Set<Long> roleIds) throws PersistenceException {
		Assert.notNull(appName, "App Name shouldn't be Null.");
		serviceLogger.info(LOG_PREFIX + "Transaction start fetching 'ActionNames' for Authenticated User with appName = <{}> and pageName = <{}>" + LOG_SUFFIX, appName, page);
		try {
			List<String> availableActions = new ArrayList<>();

			ActionCriteria criteria = new ActionCriteria();
			criteria.setActionType(Action.ActionType.MAIN);
			criteria.setAppName(appName);

			RoleCriteria roleCriteria = new RoleCriteria();
			roleCriteria.setIncludeIds(roleIds);
			criteria.setRole(roleCriteria);
			List<Action> mainPageActions = actionRepository.findAll(criteria);
			if (!CollectionUtils.isEmpty(mainPageActions)) {
				mainPageActions.forEach(action -> availableActions.add(action.getActionName()));
			}

			if (StringUtils.isNotBlank(page)) {
				criteria.setPage(page);
				criteria.setActionType(Action.ActionType.SUB);

				List<Action> subPageActions = actionRepository.findAll(criteria);
				if (!CollectionUtils.isEmpty(subPageActions)) {
					subPageActions.forEach(action -> availableActions.add(action.getActionName()));
				}
			}
			serviceLogger.info(LOG_PREFIX + "Transaction finished successfully fetching 'ActionNames' for Authenticated User with appName = <{}> and pageName = <{}>" + LOG_SUFFIX, appName, page);
			return !availableActions.isEmpty() ? availableActions : null;
		}
		catch (Exception e) {
			throw new PersistenceException(e.getMessage(), e);
		}
	}
}
