package com.tamantaw.projectx.backend.controller.mvc;

import com.tamantaw.projectx.backend.common.thymeleaf.Layout;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class HomeController extends BaseMVCController {

	@GetMapping
	public String home() {
		return "redirect:/web/sec/dashboard";
	}

	@GetMapping("/web/sec/dashboard")
	public String dashboardPage(Model model, HttpServletRequest request) {
		setAuthorities(model, "Dashboard");
		return "dashboard";
	}

	@Layout("error")
	@GetMapping("/web/pub/error/{code}")
	public String errorPage(Model model, @RequestHeader(value = HttpHeaders.REFERER, required = false) String referer, @PathVariable String code) {
		model.addAttribute("pageName", "Error !");
		model.addAttribute("referer", referer);
		return "error/" + code;
	}

	@Layout("error")
	@GetMapping("/web/pub/accessDenied")
	public String accessDenied(Model model) {
		model.addAttribute("pageName", "Error !");
		return "error/403";
	}

	@Override
	public void subInit(Model model) {

	}
}
