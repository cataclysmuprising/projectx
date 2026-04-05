package com.github.projectx.backend.utils;

import org.springframework.stereotype.Component;

import java.util.Set;

@Component("perm")
public class PermissionEvaluator {

	private static final String ALL_PERMISSION = "*";

	public boolean has(Set<String> permissions, String permission) {
		return permissions != null && (permissions.contains(ALL_PERMISSION) || permissions.contains(permission));
	}

	public boolean hasAny(Set<String> permissions, String... perms) {
		if (permissions == null) {
			return false;
		}
		if (permissions.contains(ALL_PERMISSION)) {
			return true;
		}
		for (String p : perms) {
			if (permissions.contains(p)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasAll(Set<String> permissions, String... perms) {
		if (permissions == null) {
			return false;
		}
		if (permissions.contains(ALL_PERMISSION)) {
			return true;
		}
		for (String p : perms) {
			if (!permissions.contains(p)) {
				return false;
			}
		}
		return true;
	}
}



