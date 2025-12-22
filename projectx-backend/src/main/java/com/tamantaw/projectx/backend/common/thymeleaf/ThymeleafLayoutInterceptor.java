package com.tamantaw.projectx.backend.common.thymeleaf;

import jakarta.annotation.Nonnull;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Setter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

public class ThymeleafLayoutInterceptor implements HandlerInterceptor {

	private static final String DEFAULT_PREFIX = "fragments/layouts/";
	private static final String DEFAULT_SUFFIX = "/template";
	private static final String DEFAULT_LAYOUT = DEFAULT_PREFIX + "default" + DEFAULT_SUFFIX;

	private static final String DEFAULT_VIEW_ATTRIBUTE_NAME = "view";

	private String defaultLayout = DEFAULT_LAYOUT;

	@Setter
	private String viewAttributeName = DEFAULT_VIEW_ATTRIBUTE_NAME;

	public void setDefaultLayout(String defaultLayout) {
		this.defaultLayout = DEFAULT_PREFIX + defaultLayout + DEFAULT_SUFFIX;
	}

	@Override
	public void postHandle(
			@Nonnull HttpServletRequest request,
			@Nonnull HttpServletResponse response,
			@Nonnull Object handler,
			ModelAndView modelAndView) {

		// ------------------------------------------------------------
		// 1️⃣ Must exist and have a view
		// ------------------------------------------------------------
		if (modelAndView == null || !modelAndView.hasView()) {
			return;
		}

		String originalViewName = modelAndView.getViewName();
		if (originalViewName == null) {
			return;
		}

		// ------------------------------------------------------------
		// 2️⃣ Ignore redirect / forward
		// ------------------------------------------------------------
		if (isRedirectOrForward(originalViewName)) {
			return;
		}

		// ------------------------------------------------------------
		// 3️⃣ ERROR DISPATCH (this is the missing piece)
		// ------------------------------------------------------------
		if (request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) != null) {

			// Custom error controllers may already provide the layout + content view
			if (modelAndView.getModelMap().containsKey(viewAttributeName)) {
				return;
			}

			originalViewName = normalizeViewName(originalViewName, request);

			modelAndView.setViewName("fragments/layouts/error/template");
			modelAndView.addObject(viewAttributeName, originalViewName);
			return;
		}

		// ------------------------------------------------------------
		// 4️⃣ Normal controller-based layout resolution
		// ------------------------------------------------------------
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return;
		}

		originalViewName = normalizeViewName(originalViewName, request);

		String layoutName = getLayoutName(handlerMethod);
		modelAndView.setViewName(layoutName);
		modelAndView.addObject(viewAttributeName, originalViewName);
	}

	private boolean isRedirectOrForward(String viewName) {
		return viewName.startsWith("redirect:")
				|| viewName.startsWith("forward:");
	}

	private String normalizeViewName(
			String viewName,
			HttpServletRequest request
	) {

		if (viewName == null) {
			return null;
		}

		// Remove leading slash
		if (viewName.startsWith("/")) {
			viewName = viewName.substring(1);
		}

		// Remove context path if leaked
		String contextPath = request.getContextPath();
		if (contextPath != null && !contextPath.isEmpty()) {
			if (viewName.startsWith(contextPath)) {
				viewName = viewName.substring(contextPath.length());
				if (viewName.startsWith("/")) {
					viewName = viewName.substring(1);
				}
			}
		}

		// Remove servlet path prefix if leaked (e.g. web)
		String servletPath = request.getServletPath();
		if (servletPath != null && !servletPath.isEmpty()) {
			if (viewName.startsWith(servletPath)) {
				viewName = viewName.substring(servletPath.length());
				if (viewName.startsWith("/")) {
					viewName = viewName.substring(1);
				}
			}
		}

		return viewName;
	}

	private String getLayoutName(Object handler) {
		if (handler instanceof HandlerMethod handlerMethod) {
			Layout layout = getMethodOrTypeAnnotation(handlerMethod);
			if (layout != null) {
				String value = layout.value();

				// 🚨 SAFETY: layout name must be logical, not a path
				if (value.contains("/") || value.contains("\\")) {
					return defaultLayout;
				}

				return DEFAULT_PREFIX + value + DEFAULT_SUFFIX;
			}
		}
		return defaultLayout;
	}

	private Layout getMethodOrTypeAnnotation(HandlerMethod handlerMethod) {
		Layout layout = handlerMethod.getMethodAnnotation(Layout.class);
		if (layout == null) {
			return handlerMethod.getBeanType().getAnnotation(Layout.class);
		}
		return layout;
	}
}
