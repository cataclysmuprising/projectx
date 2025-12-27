package com.tamantaw.projectx.backend.common.exceptionHandlers;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;

// To handle all exceptions out of Spring Controllers
@Component
public class ErrorHandlerFilter implements Filter {

	private static final Logger errorLogger =
			LogManager.getLogger("errorLogs." + ErrorHandlerFilter.class.getName());

	private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
	private final ObjectMapper objectMapper;

	public ErrorHandlerFilter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void doFilter(ServletRequest request,
	                     ServletResponse response,
	                     FilterChain filterChain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
		HttpServletResponse res = (HttpServletResponse) response;

		try {
			filterChain.doFilter(request, response);
		}
		catch (Exception ex) {
			errorLogger.error("Unhandled exception caught by filter for path {}", req.getRequestURI(), ex);

			if (res.isCommitted()) {
				throw ex instanceof ServletException ? (ServletException) ex : new ServletException(ex);
			}

			boolean isApi =
					req.getRequestURI().startsWith("/api/")
							|| "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));

			if (isApi) {
				ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
				problem.setTitle(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
				problem.setDetail("Unexpected server error occurred");
				problem.setInstance(URI.create(req.getRequestURI()));
				problem.setProperty("timestamp", Instant.now());
				problem.setProperty("path", req.getRequestURI());

				res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
				res.getWriter().write(objectMapper.writeValueAsString(problem));
			}
			else {
				req.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
				res.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
				req.getRequestDispatcher("/web/pub/error/500").forward(req, res);
			}
		}
	}
}
