package com.github.projectx.backend.config.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Profile("dev")
public class StaticResourceConfigDev implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/web/static/**")
				.addResourceLocations(
						"file:src/main/resources/static/",
						"classpath:/static/"
				)
				.setCacheControl(CacheControl.noStore())
				.resourceChain(false);
	}
}
