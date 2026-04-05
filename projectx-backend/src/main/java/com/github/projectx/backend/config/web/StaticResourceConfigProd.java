package com.github.projectx.backend.config.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter;
import org.springframework.web.servlet.resource.VersionResourceResolver;

import java.time.Duration;

@Configuration
@Profile({"prd", "prod", "production"})
public class StaticResourceConfigProd implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/web/static/**")
				.addResourceLocations("classpath:/static/")
				.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)))
				.resourceChain(true)
				.addResolver(
						new VersionResourceResolver()
								.addContentVersionStrategy("/**")
				);
	}

	@Bean
	public ResourceUrlEncodingFilter resourceUrlEncodingFilter() {
		return new ResourceUrlEncodingFilter();
	}
}
