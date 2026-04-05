package com.github.projectx.backend.controller.rest.web;

import com.github.projectx.backend.common.annotation.RestLoggable;
import com.github.projectx.backend.controller.rest.BaseRESTController;
import com.github.projectx.backend.service.actuator.ActuatorRedisInspectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/web/sec/settings/actuator/redis")
@RestLoggable(profile = "dev")
public class ActuatorRedisApiController extends BaseRESTController {

	private final ActuatorRedisInspectorService actuatorRedisInspectorService;

	public ActuatorRedisApiController(
			ActuatorRedisInspectorService actuatorRedisInspectorService
	) {
		this.actuatorRedisInspectorService = actuatorRedisInspectorService;
	}

	@GetMapping("/summary")
	public ResponseEntity<?> summary() {
		Map<String, Object> response = actuatorRedisInspectorService.summary();
		return ResponseEntity.ok(response);
	}

	@GetMapping("/keys")
	public ResponseEntity<?> searchKeys(
			@RequestParam(required = false) String pattern,
			@RequestParam(required = false) Integer limit
	) {
		Map<String, Object> response = actuatorRedisInspectorService.searchKeys(pattern, limit);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/delete")
	public ResponseEntity<?> deleteKeys(@RequestBody(required = false) DeleteKeysRequest request) {
		return performDelete(request);
	}

	@DeleteMapping("/delete")
	public ResponseEntity<?> deleteKeysWithDeleteMethod(@RequestBody(required = false) DeleteKeysRequest request) {
		return performDelete(request);
	}

	private ResponseEntity<?> performDelete(DeleteKeysRequest request) {
		Collection<String> keys = request == null ? List.of() : request.keys();
		String confirmation = request == null ? null : request.confirmation();
		Map<String, Object> response = actuatorRedisInspectorService.deleteKeys(keys, confirmation);
		return ResponseEntity.ok(response);
	}

	public record DeleteKeysRequest(List<String> keys, String confirmation) {
	}
}
