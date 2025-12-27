package com.tamantaw.projectx.backend.common.exceptionHandlers;

import com.tamantaw.projectx.backend.common.exception.DocumentExpiredException;
import com.tamantaw.projectx.backend.common.exception.ValidationFailedException;
import com.tamantaw.projectx.backend.controller.mvc.BaseMVCController;
import com.tamantaw.projectx.persistence.exception.BusinessException;
import com.tamantaw.projectx.persistence.exception.ConsistencyViolationException;
import com.tamantaw.projectx.persistence.exception.ContentNotFoundException;
import com.tamantaw.projectx.persistence.exception.PersistenceException;
import com.tamantaw.projectx.persistence.utils.LoggerConstants;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpSessionRequiredException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;

@ControllerAdvice
public class MVCExceptionHandler {

	private static final Logger errorLogger =
			LogManager.getLogger("errorLogs." + MVCExceptionHandler.class.getName());

	private final Environment environment;

	public MVCExceptionHandler(Environment environment) {
		this.environment = environment;
	}

	/* ============================================================
	 * 404 – NOT FOUND
	 * ============================================================ */

	@ExceptionHandler({
			NoHandlerFoundException.class,
			NoResourceFoundException.class,
			ContentNotFoundException.class
	})
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public Object handleNotFound(Exception e,
	                             Authentication auth,
	                             HttpServletRequest request) {

		logError("handleNotFound", e);

		if (isApiRequest(request)) {
			return problem(HttpStatus.NOT_FOUND, e.getMessage(),
					"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.4",
					request);
		}

		return errorView(HttpStatus.NOT_FOUND, "/error/404", auth, request);
	}

	/* ============================================================
	 * 401 – UNAUTHORIZED
	 * ============================================================ */

	@ExceptionHandler({
			HttpSessionRequiredException.class,
			SecurityException.class
	})
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public Object handleUnauthorized(Exception e,
	                                 Authentication auth,
	                                 HttpServletRequest request) {

		logError("handleUnauthorized", e);

		if (isApiRequest(request)) {
			return problem(HttpStatus.UNAUTHORIZED, e.getMessage(),
					"https://datatracker.ietf.org/doc/html/rfc7235#section-3.1",
					request);
		}

		return errorView(HttpStatus.UNAUTHORIZED, "/error/401", auth, request);
	}

	/* ============================================================
	 * 403 – FORBIDDEN
	 * ============================================================ */

	@ExceptionHandler({
			AccessDeniedException.class,
			AuthenticationException.class
	})
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public Object handleForbidden(AuthenticationException e,
	                              Authentication auth,
	                              HttpServletRequest request) {

		logError("handleForbidden", e);

		if (isApiRequest(request)) {
			return problem(HttpStatus.FORBIDDEN, e.getMessage(),
					"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.3",
					request);
		}

		return errorView(HttpStatus.FORBIDDEN, "/error/403", auth, request);
	}

	/* ============================================================
	 * 400 – BAD REQUEST
	 * ============================================================ */

	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			UnsatisfiedServletRequestParameterException.class,
			ServletRequestBindingException.class
	})
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Object handleBadRequest(Exception e,
	                               Authentication auth,
	                               HttpServletRequest request) {

		logError("handleBadRequest", e);

		if (isApiRequest(request)) {
			return problem(HttpStatus.BAD_REQUEST, e.getMessage(),
					"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.1",
					request);
		}

		return errorView(HttpStatus.BAD_REQUEST, "/error/400", auth, request);
	}

	/* ============================================================
	 * 410 – GONE
	 * ============================================================ */

	@ExceptionHandler(DocumentExpiredException.class)
	@ResponseStatus(HttpStatus.GONE)
	public ModelAndView handleGone(DocumentExpiredException e,
	                               Authentication auth,
	                               HttpServletRequest request) {

		logError("handleGone", e);
		return errorView(HttpStatus.GONE, "/error/410", auth, request);
	}

	/* ============================================================
	 * 409 – CONFLICT
	 * ============================================================ */

	@ExceptionHandler(ConsistencyViolationException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Object handleConflict(ConsistencyViolationException e,
	                             Authentication auth,
	                             HttpServletRequest request) {

		logError("handleConflict", e);

		if (isApiRequest(request)) {
			return problem(HttpStatus.CONFLICT, e.getMessage(),
					"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.8",
					request);
		}

		return errorView(HttpStatus.CONFLICT, "/error/409", auth, request);
	}

	@ExceptionHandler(ValidationFailedException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Object handleValidationFailed(
			ValidationFailedException e,
			Authentication auth,
			HttpServletRequest request
	) {
		// Validation is not a "server error"
		errorLogger.warn(LoggerConstants.LOG_BREAKER_OPEN);
		errorLogger.warn("Handler ==> handleValidationFailed");
		errorLogger.warn("Validation failed view={}", e.getErrorView());
		errorLogger.warn(LoggerConstants.LOG_BREAKER_CLOSE);

		// API / AJAX: return RFC7807 problem
		if (isApiRequest(request)) {
			return problem(
					HttpStatus.BAD_REQUEST,
					"Validation failed",
					"https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.1",
					request
			);
		}

		String view = e.getErrorView();

		// If developer passed redirect:..., respect it.
		// This works perfectly with your PRG flash attributes already set by the Aspect.
		if (view != null && view.startsWith("redirect:")) {
			ModelAndView mav = new ModelAndView(view);
			mav.setStatus(HttpStatus.BAD_REQUEST);
			return mav;
		}

		// Normal forward render: return the actual form view
		ModelAndView mav = new ModelAndView();
		mav.setViewName(view);
		mav.setStatus(HttpStatus.BAD_REQUEST);

		// Restore model attributes captured in exception
		e.getModelAttributes().forEach(mav::addObject);

		return mav;
	}


	/* ============================================================
	 * 500 – INTERNAL SERVER ERROR
	 * ============================================================ */

	@ExceptionHandler({
			BusinessException.class,
			PersistenceException.class,
			RuntimeException.class
	})
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Object handleServerError(Exception e,
	                                Authentication auth,
	                                HttpServletRequest request) {

		logError("handleServerError", e);

		if (isApiRequest(request)) {
			ProblemDetail pd = problem(HttpStatus.INTERNAL_SERVER_ERROR,
					"Internal server error",
					"https://datatracker.ietf.org/doc/html/rfc7231#section-6.6.1",
					request);
			pd.setProperty("errorCategory", "Generic");
			return pd;
		}

		return errorView(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500", auth, request);
	}

	/* ============================================================
	 * Helpers
	 * ============================================================ */

	private ProblemDetail problem(HttpStatus status,
	                              String detail,
	                              String typeUri,
	                              HttpServletRequest request) {

		ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
		pd.setTitle(status.getReasonPhrase());
		pd.setType(URI.create(typeUri));
		pd.setProperty("timestamp", Instant.now());
		pd.setProperty("path", request.getRequestURI());
		return pd;
	}

	private boolean isApiRequest(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/api/")
				|| "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}

	private void logError(String handler, Exception e) {
		errorLogger.error(LoggerConstants.LOG_BREAKER_OPEN);
		errorLogger.error("Handler ==> {}", handler);
		errorLogger.error(e.getMessage(), e);
		errorLogger.error(LoggerConstants.LOG_BREAKER_CLOSE);
	}

	private ModelAndView errorView(HttpStatus status,
	                               String view,
	                               Authentication auth,
	                               HttpServletRequest request) {

		ModelAndView mav = new ModelAndView();

		String normalizedView = normalizeErrorView(view);

		mav.setViewName("fragments/layouts/error/template");
		mav.addObject("view", normalizedView);
		mav.setStatus(status);

		mav.addObject("referer", request.getHeader("referer"));

		mav.addObject("projectVersion", BaseMVCController.getProjectVersion());
		mav.addObject("buildNumber", BaseMVCController.getBuildNumber());
		mav.addObject("appShortName", BaseMVCController.getAppShortName());
		mav.addObject("appFullName", BaseMVCController.getAppFullName());
		mav.addObject("pageName", "Error!");

		mav.addObject("isProduction",
				Arrays.asList(environment.getActiveProfiles()).contains("prd"));

		return mav;
	}

	private String normalizeErrorView(String view) {
		if (view == null) {
			return null;
		}

		// accept "/error/404" or "error/404"
		int idx = view.indexOf("/error/");
		return (idx >= 0) ? view.substring(idx + 1) : view;
	}
}
