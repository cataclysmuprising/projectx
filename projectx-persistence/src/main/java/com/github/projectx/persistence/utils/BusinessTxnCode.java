package com.github.projectx.persistence.utils;

import org.hibernate.annotations.ValueGenerationType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@ValueGenerationType(generatedBy = BusinessTxnCodeGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface BusinessTxnCode {

	/**
	 * DB sequence name
	 */
	String sequence();

	/**
	 * Prefix like CSH, TRF, WAL
	 */
	String prefix();

	/**
	 * Date pattern: yyyyMMdd, yyyyMM, etc
	 * Used only when includeDate = true
	 */
	String datePattern() default "yyyyMMdd";

	/**
	 * Whether to include date in the code
	 */
	boolean includeDate() default true;

	/**
	 * Random suffix length (0 = disabled)
	 */
	int randomLength() default 4;

	/**
	 * Character set for random suffix
	 */
	RandomType randomType() default RandomType.ALPHANUMERIC;

	/**
	 * Zero padding for sequence
	 */
	int pad() default 4;

	enum RandomType {
		NUMERIC,
		ALPHA_UPPER,
		ALPHANUMERIC
	}
}


