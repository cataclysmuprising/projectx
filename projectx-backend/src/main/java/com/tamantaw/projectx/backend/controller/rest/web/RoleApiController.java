package com.tamantaw.projectx.backend.controller.rest.web;

import com.tamantaw.projectx.backend.BackendApplication;
import com.tamantaw.projectx.backend.controller.rest.BaseRESTController;
import com.tamantaw.projectx.persistence.criteria.RoleCriteria;
import com.tamantaw.projectx.persistence.dto.RoleDTO;
import com.tamantaw.projectx.persistence.dto.base.PaginatedResult;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;

import static com.tamantaw.projectx.backend.BackendApplication.SUPER_USER_ROLE_ID;

@RestController
@RequestMapping("/api/web/sec/roles")
public class RoleApiController extends BaseRESTController {

	@Autowired
	private RoleService roleService;

	@PostMapping("/search/paging")
	public ResponseEntity<?> dataTableSearch(@RequestBody RoleCriteria criteria) throws PersistenceException {
		criteria.setAppName(BackendApplication.APP_NAME);
		HashSet<Long> superUserRoleId = new HashSet<>();
		superUserRoleId.add(SUPER_USER_ROLE_ID);
		criteria.setExcludeIds(superUserRoleId);
		PaginatedResult<RoleDTO> result = roleService.findByPaging(criteria);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@PostMapping("/search/list")
	public ResponseEntity<?> searchList(@RequestBody RoleCriteria criteria) throws PersistenceException {
		criteria.setAppName(BackendApplication.APP_NAME);
		HashSet<Long> superUserRoleId = new HashSet<>();
		superUserRoleId.add(SUPER_USER_ROLE_ID);
		criteria.setExcludeIds(superUserRoleId);
		criteria.setAppName(BackendApplication.APP_NAME);
		List<RoleDTO> results = roleService.findAll(criteria);
		return new ResponseEntity<>(results, HttpStatus.OK);
	}
}
