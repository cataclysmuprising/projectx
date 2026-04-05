package com.github.projectx.backend.common.annotation;

import com.github.projectx.backend.common.validation.BaseValidator;
import com.github.projectx.persistence.dto.response.PageMode;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface ValidateEntity {
	Class<? extends BaseValidator> validator();

	String errorView() default "";

	PageMode pageMode() default PageMode.VIEW;
}
