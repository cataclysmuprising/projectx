package com.tamantaw.projectx.backend;

import com.tamantaw.projectx.persistence.PersistenceApplication;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@Import(PersistenceApplication.class)
@EnableScheduling
public class BackendApplication extends SpringBootServletInitializer {

	public static final String APP_NAME = "projectx";
	public static final Long SUPER_USER_ID = 1001L;
	public static final Long SUPER_USER_ROLE_ID = 1L;

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@PostConstruct
	@Profile("dev")
	public void flushAllRedisCache() {
		redisConnectionFactory.getConnection().serverCommands().flushAll();
	}
}
