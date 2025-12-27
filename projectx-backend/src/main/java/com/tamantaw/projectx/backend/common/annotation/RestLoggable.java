package com.tamantaw.projectx.backend.common.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(TYPE)
public @interface RestLoggable {
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
