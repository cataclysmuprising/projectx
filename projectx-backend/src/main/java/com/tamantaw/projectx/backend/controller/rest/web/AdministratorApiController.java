package com.tamantaw.projectx.backend.controller.rest.web;

import com.tamantaw.projectx.backend.common.annotation.RestLoggable;
import com.tamantaw.projectx.backend.controller.rest.BaseRESTController;
import com.tamantaw.projectx.persistence.criteria.AdministratorCriteria;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.dto.base.PaginatedResult;
import com.tamantaw.projectx.persistence.entity.Administrator;
import com.tamantaw.projectx.persistence.entity.QAdministrator;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.ContentNotFoundException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.repository.base.UpdateSpec;
import com.tamantaw.projectx.persistence.service.AdministratorService;
import jakarta.el.MethodNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.tamantaw.projectx.backend.BackendApplication.SUPER_USER_ID;

@RestController
@RequestMapping("/api/web/sec/administrator")
@RestLoggable(profile = "dev")
public class AdministratorApiController extends BaseRESTController {

	@Autowired
	private AdministratorService administratorService;

	@PostMapping("/search/paging")
	public ResponseEntity<?> dataTableSearch(@RequestBody AdministratorCriteria criteria) throws PersistenceException {
		criteria.setExcludeIds(Set.of(SUPER_USER_ID));
		PaginatedResult<AdministratorDTO> result = administratorService.findByPaging(criteria);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@PostMapping("/search/all")
	public ResponseEntity<?> searchList(@RequestBody AdministratorCriteria criteria) throws PersistenceException {
		criteria.setExcludeIds(Set.of(SUPER_USER_ID));
		PaginatedResult<AdministratorDTO> result = administratorService.findByPaging(criteria);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@PostMapping("/{administratorId}/reset-password")
	public ResponseEntity<?> resetPassword(@PathVariable long administratorId, @RequestParam String password) throws PersistenceException, ConsistencyViolationException {
		Map<String, Object> results = new HashMap<>();
		results.put("status", "OK");
		AdministratorDTO administrator = administratorService.findById(administratorId).orElseThrow(() -> new ContentNotFoundException("Unknown Staff"));

		if (StringUtils.isBlank(password)) {
			throw new MethodNotFoundException("Password required !");
		}

		AdministratorCriteria updateCriteria = new AdministratorCriteria();
		updateCriteria.setId(administratorId);
		// Exclude SUPER_USER from Administrator List
		updateCriteria.setExcludeIds(Set.of(SUPER_USER_ID));

		UpdateSpec<Administrator> spec = (update, root) ->
				update.set(QAdministrator.administrator.password, passwordEncoder.encode(password));

		administratorService.update(spec, updateCriteria, getSignInAdministratorId());

		return new ResponseEntity<>(results, HttpStatus.OK);
	}
}
