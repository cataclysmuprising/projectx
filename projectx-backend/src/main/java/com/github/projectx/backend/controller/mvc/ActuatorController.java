package com.github.projectx.backend.controller.mvc;

import com.github.projectx.backend.common.annotation.MVCLoggable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@MVCLoggable(profile = "dev")
@RequestMapping("/web/sec/settings/actuator")
public class ActuatorController extends BaseMVCController {

	@GetMapping
	public String home() {
		return "/settings/actuator";
	}

	@Override
	public void subInit(Model model) {
		setAuthorities(model, "Actuator");
	}
}

