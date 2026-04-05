package com.github.projectx.persistence.dto.base;

import java.io.Serial;
import java.io.Serializable;

public record SortItem(
		String property,
		String direction
) implements Serializable {
	@Serial
	private static final long serialVersionUID = 1L;
}