package com.tamantaw.projectx.backend.controller.rest.web;

import com.tamantaw.projectx.backend.BackendApplication;
import com.tamantaw.projectx.backend.common.annotation.RestLoggable;
import com.tamantaw.projectx.backend.controller.rest.BaseRESTController;
import com.tamantaw.projectx.persistence.criteria.ActionCriteria;
import com.tamantaw.projectx.persistence.dto.ActionDTO;
import com.tamantaw.projectx.persistence.dto.base.PaginatedResult;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.ActionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/web/sec/action")
@RestLoggable(profile = "dev")
public class ActionApiController extends BaseRESTController {

	@Autowired
	private ActionService actionService;

	@PostMapping("/search/paging")
	public ResponseEntity<?> dataTableSearch(@RequestBody ActionCriteria criteria) throws PersistenceException {
		criteria.setAppName(BackendApplication.APP_NAME);
		PaginatedResult<ActionDTO> result = actionService.findByPaging(criteria);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@GetMapping("/pages")
	public ResponseEntity<?> pageSearch() throws PersistenceException {
		List<String> records = actionService.selectPages(BackendApplication.APP_NAME);
		return new ResponseEntity<>(records, HttpStatus.OK);
	}
}
