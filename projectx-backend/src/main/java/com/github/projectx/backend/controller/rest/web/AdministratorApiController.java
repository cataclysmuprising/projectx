package com.github.projectx.backend.controller.rest.web;

import com.github.projectx.backend.common.annotation.RestLoggable;
import com.github.projectx.backend.common.exception.RequestValidationException;
import com.github.projectx.backend.controller.rest.BaseRESTController;
import com.github.projectx.backend.controller.rest.web.request.KeysetSearchRequest;
import com.github.projectx.backend.controller.rest.web.request.PasswordResetRequest;
import com.github.projectx.backend.controller.rest.web.response.KeysetSearchResponse;
import com.github.projectx.persistence.criteria.rba.AdministratorCriteria;
import com.github.projectx.persistence.dto.base.PaginatedResult;
import com.github.projectx.persistence.dto.rba.AdministratorDTO;
import com.github.projectx.persistence.entity.rba.Administrator;
import com.github.projectx.persistence.entity.rba.QAdministrator;
import com.github.projectx.persistence.exception.ConsistencyViolationException;
import com.github.projectx.persistence.exception.ContentNotFoundException;
import com.github.projectx.persistence.exception.PersistenceException;
import com.github.projectx.persistence.repository.base.UpdateSpec;
import com.github.projectx.persistence.service.rba.AdministratorService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.github.projectx.backend.BackendApplication.SUPER_USER_ID;
import static com.github.projectx.persistence.utils.SystemActorIds.SYSTEM_ADMIN_ID;

@RestController
@RequestMapping("/api/web/sec/administrator")
@RestLoggable(profile = "dev")
public class AdministratorApiController extends BaseRESTController {

	@Autowired
	private AdministratorService administratorService;

	@PostMapping("/search/paging")
	public ResponseEntity<?> dataTableSearch(@RequestBody AdministratorCriteria criteria) throws PersistenceException {
		criteria.setExcludeIds(Set.of(SUPER_USER_ID, SYSTEM_ADMIN_ID));
		PaginatedResult<AdministratorDTO> result = administratorService.findByPaging(criteria);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/search/keyset")
	public ResponseEntity<?> keysetSearch(@RequestBody KeysetSearchRequest<AdministratorCriteria> request) throws PersistenceException {
		AdministratorCriteria criteria = request.getCriteria() != null ? request.getCriteria() : new AdministratorCriteria();
		criteria.setExcludeIds(Set.of(SUPER_USER_ID, SYSTEM_ADMIN_ID));

		KeysetSearchResponse<AdministratorDTO> result = KeysetSearchResponse.from(
				administratorService.findByKeyset(criteria, request.getAfterIdExclusive(), request.getLimit())
		);

		return ResponseEntity.ok(result);
	}

	@PostMapping("/search/all")
	public ResponseEntity<?> searchList(@RequestBody AdministratorCriteria criteria) throws PersistenceException {
		criteria.setExcludeIds(Set.of(SUPER_USER_ID, SYSTEM_ADMIN_ID));
		List<AdministratorDTO> result = administratorService.findAll(criteria);
		return ResponseEntity.ok(result);
	}

	@PostMapping("/{administratorId}/reset-password")
	public ResponseEntity<?> resetPassword(
			@PathVariable("administratorId") long administratorId,
			@RequestBody PasswordResetRequest request
	) throws PersistenceException, ConsistencyViolationException {
		Map<String, Object> results = new HashMap<>();
		results.put("status", "OK");
		String password = request == null ? null : request.getPassword();

		if (StringUtils.isBlank(password)) {
			throw new RequestValidationException("Password is required");
		}

		if (administratorId == SUPER_USER_ID || administratorId == SYSTEM_ADMIN_ID) {
			throw new RequestValidationException("SUPER_USER password cannot be reset");
		}

		administratorService.findById(administratorId).orElseThrow(() -> new ContentNotFoundException("Unknown Administrator"));

		AdministratorCriteria updateCriteria = new AdministratorCriteria();
		updateCriteria.setId(administratorId);
		// Exclude SUPER_USER from Administrator List
		updateCriteria.setExcludeIds(Set.of(SUPER_USER_ID, SYSTEM_ADMIN_ID));

		UpdateSpec<Administrator> spec = (update, root) ->
				update.set(QAdministrator.administrator.password, passwordEncoder.encode(password));

		administratorService.update(spec, updateCriteria, requireSignInAdministratorId());

		return new ResponseEntity<>(results, HttpStatus.OK);
	}
}
