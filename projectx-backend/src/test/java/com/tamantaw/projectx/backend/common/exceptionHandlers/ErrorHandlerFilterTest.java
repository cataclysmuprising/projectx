package com.tamantaw.projectx.backend.common.exceptionHandlers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ErrorHandlerFilterTest {

	private ErrorHandlerFilter filter;
	private ObjectMapper objectMapper;

	@BeforeEach
	void setUp() {
		objectMapper = new ObjectMapper();
		filter = new ErrorHandlerFilter(objectMapper);
	}

	@Test
	void whenApiRequestThrowsException_returnsProblemDetail500() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/api/test");
		request.addHeader("X-Requested-With", "XMLHttpRequest");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain failingChain = (req, res) -> {
			throw new RuntimeException("boom");
		};

		filter.doFilter(request, response, failingChain);

		assertThat(response.getStatus()).isEqualTo(500);
		assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

		ProblemDetail problemDetail = objectMapper.readValue(response.getContentAsByteArray(), ProblemDetail.class);
		assertThat(problemDetail.getStatus()).isEqualTo(500);
		assertThat(problemDetail.getTitle()).isEqualTo("Internal Server Error");
		assertThat(problemDetail.getDetail()).isEqualTo("Unexpected server error occurred");
		assertThat(problemDetail.getInstance()).hasToString("/api/test");

		Map<String, Object> properties = problemDetail.getProperties();
		assertThat(properties.get("path")).isEqualTo("/api/test");
		assertThat(properties.get("timestamp")).isNotNull();
	}

	@Test
	void whenWebRequestThrowsException_forwardsTo500Page() throws ServletException, IOException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/web/page");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain failingChain = (req, res) -> {
			throw new RuntimeException("boom");
		};

		filter.doFilter(request, response, failingChain);

		assertThat(response.getStatus()).isEqualTo(500);
		assertThat(response.getForwardedUrl()).isEqualTo("/web/pub/error/500");
		assertThat(response.getContentAsString()).isEmpty();
	}
}
