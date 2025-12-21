package com.tamantaw.projectx.backend.utils;

import jakarta.servlet.http.HttpServletRequest;

public final class SecurityUtil {

	private static final int MAX_UA_LENGTH = 255;

	private SecurityUtil() {
	}

	/* ============================================================
	 * Client IP (Cloudflare-aware)
	 * ============================================================ */

	public static String getClientIp(HttpServletRequest request) {

		// 1. Cloudflare real client IP
		String cfIp = request.getHeader("CF-Connecting-IP");
		if (isValidIp(cfIp)) {
			return cfIp;
		}

		// 2. Standard proxy header
		String xff = request.getHeader("X-Forwarded-For");
		if (xff != null && !xff.isBlank()) {
			// first IP = client
			String firstIp = xff.split(",")[0].trim();
			if (isValidIp(firstIp)) {
				return firstIp;
			}
		}

		// 3. Nginx / others
		String realIp = request.getHeader("X-Real-IP");
		if (isValidIp(realIp)) {
			return realIp;
		}

		// 4. Fallback
		return request.getRemoteAddr();
	}

	private static boolean isValidIp(String ip) {
		return ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip);
	}

	/* ============================================================
	 * Operating System
	 * ============================================================ */

	public static String getOperatingSystem(HttpServletRequest request) {
		String ua = getUserAgentHeader(request);
		String u = ua.toLowerCase();

		if (u.contains("windows")) {
			return "Windows";
		}
		if (u.contains("mac os") || u.contains("macintosh")) {
			return "macOS";
		}
		if (u.contains("android")) {
			return "Android";
		}
		if (u.contains("iphone") || u.contains("ipad")) {
			return "iOS";
		}
		if (u.contains("linux")) {
			return "Linux";
		}

		return "Unknown";
	}

	/* ============================================================
	 * Browser
	 * ============================================================ */

	public static String getUserAgent(HttpServletRequest request) {
		String ua = getUserAgentHeader(request);
		String u = ua.toLowerCase();

		if (u.contains("edg/")) {
			return extract("Edge", ua, "Edg/");
		}
		if (u.contains("chrome/")) {
			return extract("Chrome", ua, "Chrome/");
		}
		if (u.contains("firefox/")) {
			return extract("Firefox", ua, "Firefox/");
		}
		if (u.contains("safari/") && u.contains("version/")) {
			return extract("Safari", ua, "Version/");
		}
		if (u.contains("opr/") || u.contains("opera")) {
			return extract("Opera", ua, "OPR/");
		}

		return "Unknown";
	}

	/* ============================================================
	 * Helpers
	 * ============================================================ */

	private static String getUserAgentHeader(HttpServletRequest request) {
		String ua = request.getHeader("User-Agent");
		if (ua == null) {
			return "";
		}
		// limit length for safety
		return ua.length() > MAX_UA_LENGTH ? ua.substring(0, MAX_UA_LENGTH) : ua;
	}

	private static String extract(String name, String ua, String token) {
		try {
			int start = ua.indexOf(token);
			if (start == -1) {
				return name;
			}
			int end = ua.indexOf(' ', start);
			String version = (end > start)
					? ua.substring(start + token.length(), end)
					: ua.substring(start + token.length());
			return name + "-" + version;
		}
		catch (Exception e) {
			return name;
		}
	}
}
