package com.github.projectx.backend.config.web;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.ThreadContext;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Bridges Micrometer tracing context into Log4j ThreadContext (MDC).
 * <p>
 * This makes application log lines correlation-friendly by attaching
 * {@code traceId} and {@code spanId} for the current request.
 * <p>
 * Implementation detail:
 * we restore any pre-existing MDC values in {@code finally} to avoid clobbering
 * context that may already exist on reused container threads.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
public class TraceMdcFilter extends OncePerRequestFilter {

	private final Tracer tracer;

	public TraceMdcFilter(ObjectProvider<Tracer> tracerProvider) {
		tracer = tracerProvider.getIfAvailable();
	}

	private static void restoreMdcValue(String key, String oldValue) {
		if (oldValue == null) {
			ThreadContext.remove(key);
			return;
		}
		ThreadContext.put(key, oldValue);
	}

	@Override
	protected void doFilterInternal(
			@NonNull HttpServletRequest request,
			@NonNull HttpServletResponse response,
			@NonNull FilterChain filterChain
	) throws ServletException, IOException {
		String previousTraceId = ThreadContext.get("traceId");
		String previousSpanId = ThreadContext.get("spanId");

		Span span = tracer == null ? null : tracer.currentSpan();
		if (span != null) {
			ThreadContext.put("traceId", span.context().traceId());
			ThreadContext.put("spanId", span.context().spanId());
		}
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			restoreMdcValue("traceId", previousTraceId);
			restoreMdcValue("spanId", previousSpanId);
		}
	}
}
