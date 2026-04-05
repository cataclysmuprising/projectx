package com.github.projectx.backend.controller.rest.web;

import com.github.projectx.backend.BackendApplication;
import com.github.projectx.backend.common.annotation.RestLoggable;
import com.github.projectx.backend.controller.rest.BaseRESTController;
import com.github.projectx.backend.controller.rest.web.request.KeysetSearchRequest;
import com.github.projectx.backend.controller.rest.web.response.KeysetSearchResponse;
import com.github.projectx.persistence.criteria.rba.ActionCriteria;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.rba.ActionDTO;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.service.rba.ActionRouteService;
import com.github.projectx.persistence.service.rba.ActionService;
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

	@Autowired
	private ActionRouteService actionRouteService;

	@PostMapping("/search/paging")
	public ResponseEntity<?> dataTableSearch(@RequestBody ActionCriteria criteria) throws PersistenceException {
		criteria.setAppName(BackendApplication.APP_NAME);
		PaginatedResult<ActionDTO> result = actionService.findByPaging(criteria);
		actionRouteService.applyCoverage(result.getData());
		return ResponseEntity.ok(result);
	}

	@PostMapping("/search/keyset")
	public ResponseEntity<?> keysetSearch(@RequestBody KeysetSearchRequest<ActionCriteria> request) throws PersistenceException {
		ActionCriteria criteria = request.getCriteria() != null ? request.getCriteria() : new ActionCriteria();
		criteria.setAppName(BackendApplication.APP_NAME);

		var keysetResult = actionService.findByKeyset(criteria, request.getAfterIdExclusive(), request.getLimit());
		actionRouteService.applyCoverage(keysetResult.getData());
		KeysetSearchResponse<ActionDTO> result = KeysetSearchResponse.from(keysetResult);

		return ResponseEntity.ok(result);
	}

	@GetMapping("/pages")
	public ResponseEntity<?> pageSearch() throws PersistenceException {
		List<String> records = actionService.selectPages(BackendApplication.APP_NAME);
		return new ResponseEntity<>(records, HttpStatus.OK);
	}
}
