package com.github.projectx.persistence.utils;

public enum NullUpdatePolicy {
	IGNORE_NULLS,   // current behavior (PATCH)
	SET_NULLS       // overwrite DB column with NULL (PUT)
}

