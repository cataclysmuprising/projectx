package com.tamantaw.projectx.persistence.utils;

import jakarta.persistence.AttributeConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LongValueListConverter implements AttributeConverter<List<Long>, String> {
	private static final String SPLIT_CHAR = ",";

	@Override
	public String convertToDatabaseColumn(List<Long> attribute) {
		return attribute != null ? String.join(SPLIT_CHAR, String.valueOf(attribute)) : null;
	}

	@Override
	public List<Long> convertToEntityAttribute(String dbData) {
		if (StringUtils.isNotBlank(dbData)) {
			dbData = dbData.replace("[", "");
			dbData = dbData.replace("]", "");
			dbData = dbData.replace(" ", "");
			return !dbData.isEmpty() ? Arrays.stream(dbData.split(SPLIT_CHAR)).map(Long::parseLong).collect(Collectors.toList()) : null;
		}
		return null;
	}
}
