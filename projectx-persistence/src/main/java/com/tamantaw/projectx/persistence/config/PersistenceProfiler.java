package com.tamantaw.projectx.persistence.config;

import com.tamantaw.projectx.persistence.utils.YamlPropertySourceFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Configuration
public class PersistenceProfiler {

	@Configuration
	@Profile("default")
	@PropertySource(value = "classpath:persistence-dev.yml", factory = YamlPropertySourceFactory.class)
	static class Default {}

	@Configuration
	@Profile("dev")
	@PropertySource(value = "classpath:persistence-dev.yml", factory = YamlPropertySourceFactory.class)
	static class ReportingPlatformDevelopment {}

	@Configuration
	@Profile("prd")
	@PropertySource(value = "classpath:persistence-prd.yml", factory = YamlPropertySourceFactory.class)
	static class ReportingPlatformProduction {}
}
