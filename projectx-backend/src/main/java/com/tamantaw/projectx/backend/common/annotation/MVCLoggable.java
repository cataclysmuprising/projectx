package com.tamantaw.projectx.backend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface MVCLoggable {

	/**
	 * Activate logging only for matching Spring profiles
	 * Example: "dev", "local"
	 */
	String profile() default "";

	/**
	 * Log request arguments
	 */
	boolean logRequest() default true;

	/**
	 * Log response body
	 */
	boolean logResponse() default true;
}
