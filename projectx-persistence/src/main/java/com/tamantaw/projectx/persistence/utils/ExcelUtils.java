package com.tamantaw.projectx.persistence.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;

public class ExcelUtils {
	private static final ObjectMapper mapper = new ObjectMapper();

	public static <T> T getCellValue(Cell cell, Class<T> expectedType) {
		if (cell != null) {
			switch (cell.getCellType()) {
				case BOOLEAN -> {
					return mapper.convertValue(cell.getBooleanCellValue(), expectedType);
				}
				case STRING -> {
					return mapper.convertValue(cell.getRichStringCellValue().getString(), expectedType);
				}
				case NUMERIC -> {
					if (DateUtil.isCellDateFormatted(cell)) {
						return mapper.convertValue(cell.getDateCellValue(), expectedType);
					}
					else {
						return mapper.convertValue(cell.getNumericCellValue(), expectedType);
					}
				}
				case FORMULA -> {
					return mapper.convertValue(cell.getCellFormula(), expectedType);
				}
				case BLANK -> {
					return mapper.convertValue("", expectedType);
				}
				default -> {
				}
			}
		}
		return null;
	}
}
