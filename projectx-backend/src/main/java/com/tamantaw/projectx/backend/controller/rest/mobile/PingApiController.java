package com.tamantaw.projectx.backend.controller.rest.mobile;

import com.tamantaw.projectx.backend.controller.rest.BaseRESTController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile/pub")
public class PingApiController extends BaseRESTController {

	@GetMapping("/ping")
	public ResponseEntity<Map<String, String>> ping() {
		Map<String, String> response = new HashMap<>();
		response.put("status", "ok");
		response.put("message", "Server is online");
		return ResponseEntity.ok(response);
	}
}
