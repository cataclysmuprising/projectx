package com.github.projectx.backend.config.security.web;

import com.github.projectx.backend.config.security.WebSecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

public class RememberMeOldCookieErrorHandler extends GenericFilterBean {
	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException {

		HttpServletRequest httpServletRequest = ((HttpServletRequest) request);

		HttpServletResponse httpServletResponse = ((HttpServletResponse) response);
		try {
			filterChain.doFilter(request, response);
		}
		catch (Exception e) {
			Cookie rememberMeCookie = new Cookie(WebSecurityConfig.REMEMBER_ME_COOKIE, "");
			rememberMeCookie.setMaxAge(0);
			rememberMeCookie.setPath(resolveCookiePath(httpServletRequest));
			httpServletResponse.addCookie(rememberMeCookie);
			redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, "/web/pub/login");
		}
	}

	private String resolveCookiePath(HttpServletRequest request) {
		String contextPath = request == null ? "" : request.getContextPath();
		if (contextPath == null || contextPath.isBlank() || "/".equals(contextPath)) {
			return "/";
		}
		return contextPath;
	}
}
