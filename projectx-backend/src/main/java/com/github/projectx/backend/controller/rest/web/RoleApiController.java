package com.github.projectx.backend.controller.rest.web;

import com.github.projectx.backend.BackendApplication;
import com.github.projectx.backend.common.annotation.RestLoggable;
import com.github.projectx.backend.controller.rest.BaseRESTController;
import com.github.projectx.backend.controller.rest.web.request.KeysetSearchRequest;
import com.github.projectx.backend.controller.rest.web.response.KeysetSearchResponse;
import com.github.projectx.persistence.criteria.rba.RoleCriteria;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.rba.RoleDTO;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.service.rba.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

import static com.github.projectx.backend.BackendApplication.SUPER_USER_ROLE_ID;

@RestController
@RequestMapping("/api/web/sec/role")
@RestLoggable(profile = "dev")
public class RoleApiController extends BaseRESTController {

	@Autowired
	private RoleService roleService;

	@PostMapping("/search/paging")
	public ResponseEntity<?> dataTableSearch(@RequestBody RoleCriteria criteria) throws PersistenceException {
		criteria.setAppName(BackendApplication.APP_NAME);
		criteria.setExcludeIds(Set.of(SUPER_USER_ROLE_ID));
		PaginatedResult<RoleDTO> result = roleService.findByPaging(criteria);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/search/keyset")
	public ResponseEntity<?> keysetSearch(@RequestBody KeysetSearchRequest<RoleCriteria> request) throws PersistenceException {
		RoleCriteria criteria = request.getCriteria() != null ? request.getCriteria() : new RoleCriteria();
		criteria.setAppName(BackendApplication.APP_NAME);
		criteria.setExcludeIds(Set.of(SUPER_USER_ROLE_ID));

		KeysetSearchResponse<RoleDTO> result = KeysetSearchResponse.from(
				roleService.findByKeyset(criteria, request.getAfterIdExclusive(), request.getLimit())
		);

		return ResponseEntity.ok(result);
	}

	@PostMapping("/search/all")
	public ResponseEntity<?> searchAll(@RequestBody RoleCriteria criteria) throws PersistenceException {
		criteria.setAppName(BackendApplication.APP_NAME);
		criteria.setExcludeIds(Set.of(SUPER_USER_ROLE_ID));
		List<RoleDTO> result = roleService.findAll(criteria);
		return ResponseEntity.ok(result);
	}
}
