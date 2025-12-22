package com.tamantaw.projectx.backend.controller.rest.pub;

import com.fasterxml.jackson.databind.JsonNode;
import com.tamantaw.projectx.backend.controller.rest.BaseRESTController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pub/callback")
public class SampleCallbackApiController extends BaseRESTController {
	protected static final Logger callbackLogger = LogManager.getLogger("application.SampleCallback.Logs." + SampleCallbackApiController.class.getName());

	@PostMapping
	public ResponseEntity<Map<String, String>> handleCallback(@RequestBody JsonNode callbackRequest) {
		callbackLogger.info("📩 Received Sample Callback: {}", callbackRequest);

		// ✅ Always reply exactly with the expected JSON format
		Map<String, String> response = new HashMap<>();
		response.put("status", "success");

		return ResponseEntity.ok(response);
	}
}
