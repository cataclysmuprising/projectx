package com.tamantaw.projectx.backend.config.security;

import com.tamantaw.projectx.backend.config.SecurityConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {

		HttpServletRequest httpServletRequest = ((HttpServletRequest) request);

		HttpServletResponse httpServletResponse = ((HttpServletResponse) response);
		try {
			filterChain.doFilter(request, response);
		}
		catch (Exception e) {
			Cookie rememberMeCookie = new Cookie(SecurityConfig.REMEMBER_ME_COOKIE, "");
			rememberMeCookie.setMaxAge(0);
			httpServletResponse.addCookie(rememberMeCookie);
			redirectStrategy.sendRedirect(httpServletRequest, httpServletResponse, "/login");
		}
	}
}
