package com.github.projectx.backend;

import com.github.projectx.backend.config.web.ActuatorManagementDefaults;
import com.github.projectx.persistence.PersistenceApplication;
import jakarta.annotation.Nonnull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Backend web entry point.
 * <p>
 * Runtime capabilities enabled here:
 * - {@link EnableScheduling}: scheduled operational jobs
 * - {@link EnableRetry}: declarative retries (e.g., Redis transient failures)
 */
@SpringBootApplication
@Import(PersistenceApplication.class)
@EnableScheduling
@EnableRetry
public class BackendApplication extends SpringBootServletInitializer {
	/**
	 * Logical app name used by role/action registry and metadata lookups.
	 */
	public static final String APP_NAME = "projectx";
	/**
	 * Redis key for cached exchange-rate payload.
	 */
	public static final Long SUPER_USER_ID = 1L;
	public static final Long SUPER_USER_ROLE_ID = 1L;

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(BackendApplication.class);
		application.setDefaultProperties(ActuatorManagementDefaults.asMap());
		application.run(args);
	}

	@Override
	@Nonnull
	protected SpringApplicationBuilder configure(@Nonnull SpringApplicationBuilder application) {
		return application
				.sources(BackendApplication.class)
				.properties(ActuatorManagementDefaults.asMap());
	}
}
