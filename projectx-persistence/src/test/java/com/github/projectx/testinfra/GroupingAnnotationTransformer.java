package com.github.projectx.testinfra;

import org.testng.IAnnotationTransformer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Assigns exactly one TestNG group to each test method based on its name:
 * - delete > update > insert > fetch (priority order)
 * This replaces any existing groups to ensure only one group is present.
 */
public class GroupingAnnotationTransformer implements IAnnotationTransformer {

	private static final String GROUP_FETCH = "fetch";
	private static final String GROUP_INSERT = "insert";
	private static final String GROUP_UPDATE = "update";
	private static final String GROUP_DELETE = "delete";

	private static final String[] INSERT_KEYWORDS = {
			"create", "insert", "save", "register", "add", "transfer", "post", "issue", "open", "seed"
	};

	private static final String[] UPDATE_KEYWORDS = {
			"update", "edit", "modify", "patch", "reverse", "approve", "cancel", "close", "change", "set", "reset"
	};

	private static final String[] DELETE_KEYWORDS = {
			"delete", "remove", "purge", "drop", "destroy", "erase", "deactivate"
	};

	private static final String[] FETCH_KEYWORDS = {
			"find", "get", "fetch", "list", "search", "load", "query", "page", "paginate", "sort", "read"
	};

	private static boolean matchesAny(String methodNameLower, String[] keywords) {
		for (String keyword : keywords) {
			if (methodNameLower.contains(keyword)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
		if (testMethod == null) {
			return;
		}

		String methodName = testMethod.getName();
		String normalized = methodName.toLowerCase(Locale.ROOT);

		// Priority: delete > update > insert > fetch
		String group;
		if (matchesAny(normalized, DELETE_KEYWORDS)) {
			group = GROUP_DELETE;
		}
		else if (matchesAny(normalized, UPDATE_KEYWORDS)) {
			group = GROUP_UPDATE;
		}
		else if (matchesAny(normalized, INSERT_KEYWORDS)) {
			group = GROUP_INSERT;
		}
		else if (matchesAny(normalized, FETCH_KEYWORDS)) {
			group = GROUP_FETCH;
		}
		else {
			group = GROUP_FETCH; // default
		}

		annotation.setGroups(new String[]{group});
	}
}
