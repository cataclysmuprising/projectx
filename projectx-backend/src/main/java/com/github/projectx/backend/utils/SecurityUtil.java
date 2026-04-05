package com.github.projectx.backend.utils;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;

public final class SecurityUtil {

	private static final int MAX_UA_LENGTH = 255;

	private SecurityUtil() {
	}

	/* ============================================================
	 * Client IP (Cloudflare-aware)
	 * ============================================================ */

	public static String getClientIp(HttpServletRequest request) {
		String remoteAddr = trimToNull(request.getRemoteAddr());
		if (!isTrustedProxy(remoteAddr)) {
			return remoteAddr == null ? "0.0.0.0" : remoteAddr;
		}

		// 1. Cloudflare real client IP
		String cfIp = trimToNull(request.getHeader("CF-Connecting-IP"));
		if (isValidIp(cfIp)) {
			return cfIp;
		}

		// 2. Standard proxy header
		String xff = trimToNull(request.getHeader("X-Forwarded-For"));
		if (xff != null && !xff.isBlank()) {
			// first IP = client
			String firstIp = trimToNull(xff.split(",")[0]);
			if (isValidIp(firstIp)) {
				return firstIp;
			}
		}

		// 3. Nginx / others
		String realIp = trimToNull(request.getHeader("X-Real-IP"));
		if (isValidIp(realIp)) {
			return realIp;
		}

		// 4. Fallback
		return remoteAddr == null ? "0.0.0.0" : remoteAddr;
	}

	private static boolean isValidIp(String ip) {
		if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
			return false;
		}
		try {
			InetAddress address = InetAddress.getByName(ip);
			return address != null;
		}
		catch (Exception e) {
			return false;
		}
	}

	private static boolean isTrustedProxy(String remoteAddr) {
		if (!isValidIp(remoteAddr)) {
			return false;
		}
		try {
			InetAddress address = InetAddress.getByName(remoteAddr);
			return address.isAnyLocalAddress()
					|| address.isLoopbackAddress()
					|| address.isLinkLocalAddress()
					|| address.isSiteLocalAddress();
		}
		catch (Exception e) {
			return false;
		}
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
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
