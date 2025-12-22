package com.tamantaw.projectx.backend.common.exceptionHandlers;

import com.tamantaw.projectx.backend.controller.mvc.BaseMVCController;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Arrays;

@Controller
@Primary
@RequestMapping("${spring.web.error.path:/error}")
public class CustomErrorController implements ErrorController {

	private final Environment environment;

	public CustomErrorController(Environment environment) {
		this.environment = environment;
	}

	@RequestMapping(produces = "text/html")
	public ModelAndView handleError(HttpServletRequest request, HttpServletResponse response) {

		HttpStatus status = resolveStatus(request);
		response.setStatus(status.value());

		ModelAndView mv = new ModelAndView("fragments/layouts/error/template");

		// page fragment resolver (Thymeleaf layout style)
		mv.addObject("view", "error/" + status.value());

		// common layout attributes
		mv.addObject("referer", request.getHeader(HttpHeaders.REFERER));
		mv.addObject("projectVersion", BaseMVCController.getProjectVersion());
		mv.addObject("buildNumber", BaseMVCController.getBuildNumber());
		mv.addObject("appShortName", BaseMVCController.getAppShortName());
		mv.addObject("appFullName", BaseMVCController.getAppFullName());
		mv.addObject("pageName", "Error " + status.value());

		mv.addObject(
				"isProduction",
				Arrays.asList(environment.getActiveProfiles()).contains("prd")
		);

		return mv;
	}

	// ---------------------------------------------------------------------
	// Internal helpers
	// ---------------------------------------------------------------------

	private HttpStatus resolveStatus(HttpServletRequest request) {
		Object statusCode =
				request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

		if (ObjectUtils.isEmpty(statusCode)) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}

		try {
			return HttpStatus.valueOf(Integer.parseInt(statusCode.toString()));
		}
		catch (Exception ex) {
			return HttpStatus.INTERNAL_SERVER_ERROR;
		}
	}
}
