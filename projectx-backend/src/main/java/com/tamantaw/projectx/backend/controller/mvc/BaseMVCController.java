package com.tamantaw.projectx.backend.controller.mvc;

import com.tamantaw.projectx.backend.BackendApplication;
import com.tamantaw.projectx.backend.common.response.PageMessage;
import com.tamantaw.projectx.backend.common.response.PageMessageStyle;
import com.tamantaw.projectx.backend.config.security.AuthenticatedClient;
import com.tamantaw.projectx.persistence.dto.AdministratorDTO;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.service.ActionService;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public abstract class BaseMVCController {
	public static final Long SUPER_USER_ID = 1001L;
	public static final Long SUPER_USER_ROLE_ID = 1L;
	@Getter
	private static String projectVersion;
	@Getter
	private static String buildNumber;
	@Autowired
	protected Environment environment;
	@Autowired
	protected MessageSource messageSource;
	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	private ActionService actionService;
	@Autowired
	private ServletContext servletContext;

	public BaseMVCController() {
		projectVersion = "1.0";
		buildNumber = DateTimeFormatter.ofPattern("ddMMyyyy-HHmm").format(LocalDateTime.now());
	}

	public static String getAppShortName() {
		return "Reporting";
	}

	public static String getAppFullName() {
		return "Reporting Platform";
	}

	@ModelAttribute
	public void init(Model model) {
		AuthenticatedClient loginAdmin = getSignInClientInfo();
		if (loginAdmin != null) {
			AdministratorDTO administrator = loginAdmin.getUserDetail();
			model.addAttribute("loginUserName", administrator.getName());
			model.addAttribute("loginUserId", administrator.getId());
		}
		setMetaData(model);
		subInit(model);
	}

	protected void setAuthorities(Model model, String pageName) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null) {
			model.addAttribute("pageName", pageName);
			AuthenticatedClient loginAdmin = getSignInClientInfo();
			if (loginAdmin != null) {
				HashMap<String, Boolean> accessments = new HashMap<>();
				List<String> actions;
				try {
					actions = actionService.selectAvailableActionsForUser(pageName, BackendApplication.APP_NAME, loginAdmin.getRoleIds());
				}
				catch (PersistenceException e) {
					String errorMessage = "Failed to fetch available actions for administrator <" + loginAdmin + "> with given pageName = <" + pageName + "> ";
					throw new RuntimeException(errorMessage);
				}
				if (actions != null) {
					actions.forEach(actionName -> {
						model.addAttribute(actionName, true);
						accessments.put(actionName, true);
					});
					model.addAttribute("accessments", accessments);
				}
			}
		}
	}

	protected String getApplicationContextPath(HttpServletRequest request) {
		return request.getRequestURL().toString().replace(request.getServletPath(), "");
	}

	protected Long getSignInClientId() {
		AuthenticatedClient loginUser = getSignInClientInfo();
		if (loginUser != null) {
			return loginUser.getId();
		}
		return null;
	}

	protected AuthenticatedClient getSignInClientInfo() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getPrincipal() == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
			return null;
		}
		return mapper.convertValue(auth.getPrincipal(), AuthenticatedClient.class);
	}

	private void setMetaData(Model model) {
		model.addAttribute("contextPath", servletContext.getContextPath());
		model.addAttribute("projectVersion", getProjectVersion());
		model.addAttribute("buildNumber", getBuildNumber());
		model.addAttribute("appShortName", getAppShortName());
		model.addAttribute("appFullName", getAppFullName());
		model.addAttribute("isProduction", "prd".equals(environment.getActiveProfiles()[0]));
	}

	Map<String, Object> setAjaxFormFieldErrors(Errors errors, String errorKeyPrefix) {
		Map<String, Object> response = new HashMap<>();
		Map<String, String> fieldErrors = new HashMap<>();
		response.put("status", HttpStatus.METHOD_NOT_ALLOWED);

		List<FieldError> errorFields = errors.getFieldErrors();
		errorFields.forEach(item -> {
			if (errorKeyPrefix != null) {
				if (!fieldErrors.containsKey(errorKeyPrefix + item.getField())) {
					fieldErrors.put(errorKeyPrefix + item.getField(), item.getDefaultMessage());
				}
			}
			else {
				if (!fieldErrors.containsKey(item.getField())) {
					fieldErrors.put(item.getField(), item.getDefaultMessage());
				}
			}
		});
		response.put("fieldErrors", fieldErrors);
		response.put("type", "validationError");
		setAjaxPageMessage(response, "Validation Error", "Validation.common.Page.ValidationErrorMessage", PageMessageStyle.ERROR);
		return response;
	}

	protected void setPageMessage(Model model, String messageTitle, String messageBody, PageMessageStyle style) {
		model.addAttribute("pageMessage", new PageMessage(messageTitle, messageBody, style.getValue()));
	}

	protected void setPageMessage(Model model, String messageTitle, String messageCode, PageMessageStyle style, Object... messageParams) {
		model.addAttribute("pageMessage", new PageMessage(messageTitle, messageSource.getMessage(messageCode, messageParams, Locale.ENGLISH), style.getValue()));
	}

	protected void setPageMessage(RedirectAttributes redirectAttributes, String messageTitle, String messageCode, PageMessageStyle style, Object... messageParams) {
		redirectAttributes.addFlashAttribute("pageMessage", new PageMessage(messageTitle, messageSource.getMessage(messageCode, messageParams, Locale.ENGLISH), style.getValue()));
	}

	protected void setPageMessage(boolean raw, RedirectAttributes redirectAttributes, String messageTitle, String message, PageMessageStyle style, Object... messageParams) {
		redirectAttributes.addFlashAttribute("pageMessage", new PageMessage(messageTitle, message, style.getValue()));
	}

	protected Map<String, Object> setAjaxPageMessage(Map<String, Object> response, String messageTitle, String messageCode, PageMessageStyle style, Object... messageParams) {
		if (response == null) {
			response = new HashMap<>();
		}
		response.put("pageMessage", new PageMessage(messageTitle, messageSource.getMessage(messageCode, messageParams, Locale.ENGLISH), style.getValue()));
		return response;
	}

	protected boolean containsIgnoreCase(List<String> list, String soughtFor) {
		for (String current : list) {
			if (current.equalsIgnoreCase(soughtFor)) {
				return true;
			}
		}
		return false;
	}

	public abstract void subInit(Model model);
}
