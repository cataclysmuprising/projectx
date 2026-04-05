package com.github.projectx.persistence.utils;

import java.time.LocalDateTime;
import java.time.YearMonth;

public class DateTimeUtils {

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public static LocalDateTime startOfDay(int year, int month) {
		return LocalDateTime.of(year, month, 1, 0, 0, 0, 0);
	}

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	public static LocalDateTime endOfDay(int year, int month) {
		YearMonth yearMonth = YearMonth.of(year, month);
		return LocalDateTime.of(year, month, yearMonth.lengthOfMonth(), 23, 59, 59, 999999999);
	}
}

