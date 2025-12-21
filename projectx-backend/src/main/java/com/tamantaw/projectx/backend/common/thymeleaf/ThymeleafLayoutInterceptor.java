package com.tamantaw.projectx.backend.common.thymeleaf;

import jakarta.annotation.Nonnull;
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
	public void postHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler, ModelAndView modelAndView) {
		if (modelAndView == null || !modelAndView.hasView()) {
			return;
		}
		String originalViewName = modelAndView.getViewName();
		assert originalViewName != null;
		if (isRedirectOrForward(originalViewName)) {
			return;
		}
		String layoutName = getLayoutName(handler);
		modelAndView.setViewName(layoutName);
		modelAndView.addObject(viewAttributeName, originalViewName);
	}

	private boolean isRedirectOrForward(String viewName) {
		return viewName.startsWith("redirect:") || viewName.startsWith("forward:");
	}

	private String getLayoutName(Object handler) {
		if (handler instanceof HandlerMethod handlerMethod) {
			Layout layout = getMethodOrTypeAnnotation(handlerMethod);
			if (layout != null) {
				return DEFAULT_PREFIX + layout.value() + DEFAULT_SUFFIX;
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
