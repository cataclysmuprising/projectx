package com.tamantaw.projectx.backend.controller.mvc;

import com.tamantaw.projectx.backend.common.thymeleaf.Layout;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
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
		return "redirect:/sec/dashboard";
	}

	@GetMapping("/sec/dashboard")
	public String dashboardPage(Model model, HttpServletRequest request) throws PersistenceException {
		setAuthorities(model, "Dashboard");
		return "dashboard";
	}

	@Layout("error")
	@GetMapping("/error/{code}")
	public String errorPage(Model model, @RequestHeader(value = HttpHeaders.REFERER, required = false) String referer, @PathVariable String code) {
		model.addAttribute("pageName", "Error !");
		model.addAttribute("referer", referer);
		return "error/" + code;
	}

	@Layout("error")
	@GetMapping("/404.html")
	public String pageNotFound(Model model) {
		model.addAttribute("pageName", "Error !");
		return "error/404";
	}

	@Layout("error")
	@GetMapping("/accessDenied")
	public String accessDenied(Model model) {
		model.addAttribute("pageName", "Error !");
		return "error/403";
	}

	@Override
	public void subInit(Model model) {

	}
}
