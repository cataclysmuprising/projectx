package com.github.projectx.backend.controller.mvc;

import com.github.projectx.backend.BackendApplication;
import com.github.projectx.backend.config.security.web.AuthenticatedClient;
import com.github.projectx.backend.utils.ActionRegistry;
import com.github.projectx.persistence.dto.rba.AdministratorDTO;
import com.github.projectx.persistence.dto.response.PageMessage;
import com.github.projectx.persistence.dto.response.PageMessageStyle;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public abstract class BaseMVCController {

	/* ============================================================
	 * Application metadata (immutable)
	 * ============================================================ */

	private static final String PROJECT_VERSION;
	private static final String BUILD_NUMBER;

	static {
		PROJECT_VERSION = "1.0"; // ideally from build-info.properties
		BUILD_NUMBER = DateTimeFormatter
				.ofPattern("ddMMyyyy-HHmm")
				.format(LocalDateTime.now());
	}

	@Autowired
	protected MessageSource messageSource;
	@Autowired
	protected ObjectMapper mapper;
	@Autowired
	private ActionRegistry actionRegistry;

	public static String getProjectVersion() {
		return PROJECT_VERSION;
	}

	public static String getBuildNumber() {
		return BUILD_NUMBER;
	}

	public static String getAppShortName() {
		return "ProjectX";
	}

	public static String getAppFullName() {
		return "ProjectX";
	}

	/* ============================================================
	 * Global model initializer
	 * ============================================================ */

	@ModelAttribute
	public final void init(Model model, HttpServletRequest request) {
		AuthenticatedClient loginUser = getSignInAdministratorInfo();
		if (loginUser != null && loginUser.getUserDetail() != null) {
			AdministratorDTO admin = loginUser.getUserDetail();
			model.addAttribute("loginAdministratorName", admin.getName());
			model.addAttribute("loginAdministratorId", admin.getId());
		}

		setMetaData(model, request);
		subInit(model);
	}


	/* ============================================================
	 * Authorization / permissions
	 * ============================================================ */

	protected void setAuthorities(Model model, String pageName) {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return;
		}

		model.addAttribute("pageName", pageName);

		AuthenticatedClient loginAdministrator = getSignInAdministratorInfo();
		if (loginAdministrator == null) {
			return;
		}

		Set<String> permissions = actionRegistry.resolveAvailableActionsForUser(
				BackendApplication.APP_NAME,
				pageName,
				loginAdministrator.getRoleNames()
		);

		if (permissions == null || permissions.isEmpty()) {
			model.addAttribute("permissions", Set.of());
			return;
		}

		model.addAttribute("permissions", permissions);
	}

	/* ============================================================
	 * Security helpers
	 * ============================================================ */

	protected Long getSignInAdministratorId() {
		AuthenticatedClient user = getSignInAdministratorInfo();
		return (user != null) ? user.getId() : null;
	}

	protected AuthenticatedClient getSignInAdministratorInfo() {

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) {
			return null;
		}

		Object principal = auth.getPrincipal();
		if (principal == null || "anonymousUser".equals(principal)) {
			return null;
		}

		if (principal instanceof AuthenticatedClient ac) {
			return ac;
		}

		// fallback (legacy compatibility)
		try {
			return mapper.convertValue(principal, AuthenticatedClient.class);
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/* ============================================================
	 * Metadata helpers
	 * ============================================================ */

	private void setMetaData(Model model, HttpServletRequest request) {

		model.addAttribute("contextPath", request.getContextPath());
		model.addAttribute("projectVersion", getProjectVersion());
		model.addAttribute("buildNumber", getBuildNumber());
		model.addAttribute("appShortName", getAppShortName());
		model.addAttribute("appFullName", getAppFullName());

		boolean isProduction = false;

		WebApplicationContext ctx =
				WebApplicationContextUtils.getWebApplicationContext(
						request.getServletContext()
				);

		if (ctx != null) {
			Environment env = ctx.getEnvironment();
			isProduction = env.matchesProfiles("prd", "prod", "production");
		}

		model.addAttribute("isProduction", isProduction);
	}



	/* ============================================================
	 * Messaging helpers (i18n-safe)
	 * ============================================================ */

	protected void setPageMessage(
			Model model,
			String title,
			String messageCode,
			PageMessageStyle style,
			Object... params
	) {
		Locale locale = LocaleContextHolder.getLocale();
		model.addAttribute(
				"pageMessage",
				new PageMessage(
						title,
						messageSource.getMessage(messageCode, params, locale),
						style.getValue()
				)
		);
	}

	protected void setPageMessage(
			RedirectAttributes redirectAttributes,
			String title,
			String messageCode,
			PageMessageStyle style,
			Object... params
	) {
		Locale locale = LocaleContextHolder.getLocale();
		redirectAttributes.addFlashAttribute(
				"pageMessage",
				new PageMessage(
						title,
						messageSource.getMessage(messageCode, params, locale),
						style.getValue()
				)
		);
	}

	/* ============================================================
	 * AJAX helpers (legacy but safer)
	 * ============================================================ */

	protected Map<String, Object> setAjaxFormFieldErrors(
			Errors errors,
			String errorKeyPrefix
	) {

		Map<String, Object> response = new HashMap<>();
		Map<String, String> fieldErrors = new HashMap<>();

		for (FieldError fe : errors.getFieldErrors()) {
			String key = (errorKeyPrefix != null)
					? errorKeyPrefix + fe.getField()
					: fe.getField();
			fieldErrors.putIfAbsent(key, fe.getDefaultMessage());
		}

		response.put("status", HttpStatus.BAD_REQUEST);
		response.put("type", "validationError");
		response.put("fieldErrors", fieldErrors);

		setAjaxPageMessage(
				response,
				"Validation Error",
				"Validation.common.Page.ValidationErrorMessage",
				PageMessageStyle.ERROR
		);

		return response;
	}

	protected Map<String, Object> setAjaxPageMessage(
			Map<String, Object> response,
			String title,
			String messageCode,
			PageMessageStyle style,
			Object... params
	) {
		Locale locale = LocaleContextHolder.getLocale();
		response.put(
				"pageMessage",
				new PageMessage(
						title,
						messageSource.getMessage(messageCode, params, locale),
						style.getValue()
				)
		);
		return response;
	}

	/* ============================================================
	 * Utilities
	 * ============================================================ */

	protected boolean containsIgnoreCase(Collection<String> list, String soughtFor) {
		if (list == null || soughtFor == null) {
			return false;
		}
		return list.stream().anyMatch(s -> s.equalsIgnoreCase(soughtFor));
	}

	protected void reloadActionRegistry() {
		actionRegistry.reload();
	}

	/* ============================================================
	 * Extension point
	 * ============================================================ */

	protected abstract void subInit(Model model);
}
