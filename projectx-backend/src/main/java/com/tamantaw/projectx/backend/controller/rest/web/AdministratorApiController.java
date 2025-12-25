package com.tamantaw.projectx.backend.controller.rest.web;

import com.tamantaw.projectx.backend.controller.rest.BaseRESTController;
import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.dto.base.PaginatedResult;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.AdministratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;

import static com.tamantaw.projectx.backend.BackendApplication.SUPER_USER_ID;

@RestController
@RequestMapping("/api/web/sec/administrator")
public class AdministratorApiController extends BaseRESTController {

	@Autowired
	private AdministratorService administratorService;

	@PostMapping("/search/paging")
	public ResponseEntity<?> dataTableSearch(@RequestBody AdministratorCriteria criteria) throws PersistenceException {
		HashSet<Long> superUserId = new HashSet<>();
		superUserId.add(SUPER_USER_ID);
		criteria.setExcludeIds(superUserId);
		PaginatedResult<AdministratorDTO> result = administratorService.findByPaging(criteria);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
