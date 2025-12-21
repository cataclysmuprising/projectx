package com.tamantaw.projectx.backend.common.exceptionHandlers;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;

// To handle all exceptions out of Spring Controllers
@Component
public class ErrorHandlerFilter implements Filter {

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private final ObjectMapper objectMapper;

	public ErrorHandlerFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void doFilter(ServletRequest request,
	                     ServletResponse response,
	                     FilterChain filterChain) throws IOException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		try {
			filterChain.doFilter(request, response);
		}
		catch (Exception ex) {
			if (res.isCommitted()) {
				return;
			}

			boolean isApi =
					req.getRequestURI().startsWith("/api/")
							|| "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));

			if (isApi) {
				ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
				problem.setTitle(HttpStatus.FORBIDDEN.getReasonPhrase());
				problem.setDetail("Unknown exception occurred!");
				problem.setProperty("timestamp", Instant.now());
				problem.setProperty("path", req.getRequestURI());

				res.setStatus(HttpStatus.FORBIDDEN.value());
				res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
				res.getWriter().write(objectMapper.writeValueAsString(problem));
			}
			else {
				redirectStrategy.sendRedirect(req, res, "/error/500");
			}
		}
	}
}

