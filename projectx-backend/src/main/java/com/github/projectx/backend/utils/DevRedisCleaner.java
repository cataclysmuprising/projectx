package com.github.projectx.backend.utils;

import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevRedisCleaner {

	private static final Logger log = LogManager.getLogger(DevRedisCleaner.class);

	private final RedisConnectionFactory redisConnectionFactory;

	public DevRedisCleaner(RedisConnectionFactory redisConnectionFactory) {
		this.redisConnectionFactory = redisConnectionFactory;
	}

	@PostConstruct
	public void flushAllRedisCache() {
		try (var connection = redisConnectionFactory.getConnection()) {
			connection.serverCommands().flushAll();
			log.warn("🚨 DEV ONLY: Redis FLUSHALL executed");
		}
		catch (Exception ex) {
			log.warn("DEV ONLY: Redis FLUSHALL skipped because Redis is unavailable: {}", ex.toString());
		}
	}
}
