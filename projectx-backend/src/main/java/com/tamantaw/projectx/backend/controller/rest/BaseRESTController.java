package com.tamantaw.projectx.backend.controller.rest;

import com.tamantaw.projectx.backend.config.security.AuthenticatedClient;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.ObjectMapper;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class BaseRESTController {

	protected static final Logger mobileAppLogger = LogManager.getLogger("application.MOBILE_APP.Logs." + BaseRESTController.class.getName());

	@Autowired
	protected PasswordEncoder passwordEncoder;

	@Autowired
	private ObjectMapper mapper;

	protected Long getSignInAdministratorId() {
		AuthenticatedClient loginAdmin = getSignInAdministrator();
		if (loginAdmin != null) {
			return loginAdmin.getId();
		}
		return null;
	}

	protected AuthenticatedClient getSignInAdministrator() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || auth.getPrincipal() == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
			return null;
		}
		return mapper.convertValue(auth.getPrincipal(), AuthenticatedClient.class);
	}

	protected boolean containsIgnoreCase(List<String> list, String soughtFor) {
		for (String current : list) {
			if (current.equalsIgnoreCase(soughtFor)) {
				return true;
			}
		}
		return false;
	}

	protected <T, U> List<U> convertList(List<T> sourceList, Class<U> targetClass) {
		return sourceList.stream().map(source -> {
			return mapper.convertValue(source, targetClass);
		}).collect(Collectors.toList());
	}

	// ===================== UTILITIES (logging + helpers) =====================

	protected String newRid() {
		// Short, log-friendly request id
		return UUID.randomUUID().toString().substring(0, 8);
	}

	//@formatter:off
	protected void logOpen(String rid, String className, String method, HttpServletRequest req, Object bodyIfAny) {
		// Put useful MDC so every downstream log line can include them via pattern: %X{rid} %X{cls} %X{mth}
		ThreadContext.put("rid", rid);
		ThreadContext.put("cls", className);
		ThreadContext.put("mth", method);

		String uri = (req != null) ? req.getRequestURI() : "(unknown)";
		String qs  = (req != null && req.getQueryString() != null) ? ("?" + req.getQueryString()) : "";
		String ip  = getClientIp();
		String ua  = getUserAgent();

		mobileAppLogger.info(
				"""
				
				====================[ OPEN {}#{} ]==================== rid={}
				Class: {}
				URI: {}{}
				IP: {}
				User-Agent: {}
				Headers:
				{}
				Body:
				{}
				""",
				className, method, rid,
				className,
				uri, qs,
				ip,
				ua,
				headersAsString(req),
				safeJson(bodyIfAny)
		);
	}

	protected void logClose(String rid, String className, String method, long startedNanos, Object resultBody, HttpStatus status) {
		long tookMs = (System.nanoTime() - startedNanos) / 1_000_000L;

		mobileAppLogger.info(
				"""

					ResultStatus: {} (took {} ms) rid={} class={} method={}
					ResultBody:
					{}
					====================[ CLOSE {}#{} ]====================
					""",
				status.value(), tookMs, rid, className, method,
				safeJson(resultBody),
				className, method
		);

		// Clean up MDC
		ThreadContext.remove("rid");
		ThreadContext.remove("cls");
		ThreadContext.remove("mth");
	}
	//@formatter:on

	protected String headersAsString(HttpServletRequest req) {
		if (req == null) {
			return "(no request)";
		}
		StringBuilder sb = new StringBuilder();
		Enumeration<String> names = req.getHeaderNames();
		while (names != null && names.hasMoreElements()) {
			String name = names.nextElement();
			String value = req.getHeader(name);
			if (value == null) {
				value = "";
			}
			if (value.length() > 30) {
				value = value.substring(0, 28) + "...";
			}
			String lower = name.toLowerCase(Locale.ROOT);
			sb.append("  ").append(name).append(": ").append(value).append("\n");
		}
		return sb.toString();
	}

	protected String safeJson(Object body) {
		if (body == null) {
			return "(null)";
		}
		return mapper.writeValueAsString(body);
	}

	protected boolean isNumericOnly(String input) {
		return Pattern.matches("^\\d+$", input);
	}

	protected boolean isAlphaNumerics(String checkStr) {
		return Pattern.matches("^[a-zA-Z_0-9 ]+$", checkStr);
	}

	protected String getClientIp() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

		// 1) Extract original IP (same as before)
		String h = null;
		try {
			h = StringUtils.trimToEmpty(request.getHeader("CF-Connecting-IP"));
			if (StringUtils.isBlank(h)) {
				h = StringUtils.trimToEmpty(request.getHeader("X-Real-IP"));
			}
			if (StringUtils.isBlank(h)) {
				h = StringUtils.trimToEmpty(request.getHeader("X-Forwarded-For"));
			}
		}
		catch (Exception ignored) {
		}

		String ip;
		if (StringUtils.isNotBlank(h)) {
			ip = h.split(",")[0].trim();
		}
		else {
			try {
				ip = request.getRemoteAddr();
			}
			catch (Exception e) {
				ip = "0.0.0.0";
			}
		}

		// 2) Normalize IPv6 → /64 prefix
		try {
			InetAddress addr = InetAddress.getByName(ip);

			if (addr instanceof Inet6Address) {
				byte[] full = addr.getAddress();      // 16 bytes
				byte[] prefix = Arrays.copyOf(full, 8); // /64 prefix

				StringBuilder sb = new StringBuilder("v6pfx:");
				for (byte b : prefix) {
					sb.append(String.format("%02x", b));
				}
				return sb.toString();  // normalized IPv6
			}
		}
		catch (Exception ignored) {
		}

		// 3) IPv4 or invalid → return as-is
		return ip;
	}

	protected String getUserAgent() {
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
		return request.getHeader("User-Agent");
	}
}

