package com.tamantaw.projectx.persistence.service;

import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.entity.Action;
import com.tamantaw.projectx.persistence.entity.QAction;
import com.tamantaw.projectx.persistence.mapper.ActionMapper;
import com.tamantaw.projectx.persistence.repository.ActionRepository;
import com.tamantaw.projectx.persistence.service.base.BaseService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ActionService
		extends BaseService<
		Action,
		QAction,
		ActionCriteria,
		ActionDTO,
		ActionMapper> {

	private static final Logger log =
			LogManager.getLogger("serviceLogs." + ActionService.class.getSimpleName());

	@Autowired
	public ActionService(ActionRepository actionRepository, ActionMapper mapper) {
		super(actionRepository, mapper);
	}
}
