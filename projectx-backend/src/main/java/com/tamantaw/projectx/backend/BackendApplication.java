package com.tamantaw.projectx.backend;

import com.tamantaw.projectx.persistence.PersistenceApplication;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@SpringBootApplication
@Import(PersistenceApplication.class)
public class BackendApplication extends SpringBootServletInitializer {

	public static final String APP_NAME = "projectx";

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(BackendApplication.class);
	}

	@PostConstruct
	public void flushAllRedisCache() {
		redisConnectionFactory.getConnection().serverCommands().flushAll();
	}
}
