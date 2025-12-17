package com.tamantaw.projectx.persistence.utils;

import java.time.LocalDateTime;
import java.time.YearMonth;

public class DateTimeUtils {

	public static LocalDateTime startOfDay(int year, int month) {
		return LocalDateTime.of(year, month, 1, 0, 0, 0, 0);
	}

	public static LocalDateTime endOfDay(int year, int month) {
		YearMonth yearMonth = YearMonth.of(year, month);
		return LocalDateTime.of(year, month, yearMonth.lengthOfMonth(), 23, 59, 59, 999999999);
	}
}
