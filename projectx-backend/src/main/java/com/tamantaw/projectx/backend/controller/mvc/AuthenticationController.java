package com.tamantaw.projectx.backend.controller.mvc;

import com.tamantaw.projectx.backend.common.thymeleaf.Layout;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Locale;

@Controller
public class AuthenticationController extends BaseMVCController {

	@GetMapping("/web/pub/login")
	@Layout("plain")
	public String login(Model model, @RequestParam(required = false, name = "error") String error) {
		Locale locale = Locale.ENGLISH;
		if (error != null) {
			model.addAttribute("messageStyle", "alert-danger");
			switch (error) {
				case "account-disabled" -> model.addAttribute("pageMessage", messageSource.getMessage("Serverity.common.auth.message.disabled", null, locale));
				case "account-locked" -> model.addAttribute("pageMessage", messageSource.getMessage("Serverity.common.auth.message.locked", null, locale));
				case "account-expired" -> model.addAttribute("pageMessage", messageSource.getMessage("Serverity.common.auth.message.expired", null, locale));
				default -> model.addAttribute("pageMessage", messageSource.getMessage("Serverity.common.auth.message.badCredentials", null, locale));
			}
		}
		return "login";
	}

	@Override
	public void subInit(Model model) {

	}
}
