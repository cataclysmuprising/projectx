package com.github.projectx.backend.config.retry;

import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.lang.annotation.*;

/**
 * Standard retry profile for transient Redis failures.
 * <p>
 * This annotation centralizes retry policy for Redis read/write operations so
 * services do not copy/paste {@link Retryable} attributes.
 * <p>
 * Runtime tuning is externalized in configuration:
 * <ul>
 *   <li>{@code app.retry.redis.max-attempts}</li>
 *   <li>{@code app.retry.redis.delay-ms}</li>
 *   <li>{@code app.retry.redis.multiplier}</li>
 *   <li>{@code app.retry.redis.max-delay-ms}</li>
 * </ul>
 * <p>
 * Requires {@code @EnableRetry} on the application configuration.
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Retryable(
		retryFor = {RedisConnectionFailureException.class, RedisSystemException.class, QueryTimeoutException.class},
		maxAttemptsExpression = "${app.retry.redis.max-attempts:3}",
		backoff = @Backoff(
				delayExpression = "${app.retry.redis.delay-ms:200}",
				multiplierExpression = "${app.retry.redis.multiplier:2.0}",
				maxDelayExpression = "${app.retry.redis.max-delay-ms:2000}"
		)
)
public @interface RedisTransientRetry {
}
