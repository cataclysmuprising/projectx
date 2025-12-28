package com.tamantaw.projectx.backend.config;

import com.tamantaw.projectx.backend.config.security.*;
import com.tamantaw.projectx.backend.utils.ActionRegistry;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

import java.time.Duration;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	public static final String REMEMBER_ME_COOKIE = "projectx_backend_rbm";

	private final UrlRequestAuthenticationSuccessHandler urlSuccessHandler;
	private final RememberMeAuthenticationSuccessHandler rememberMeSuccessHandler;
	private final CustomAuthenticationFailureHandler failureHandler;
	private final ActionRegistry actionRegistry;
	private final Environment environment;

	@Value("${security.rememberme.key}")
	private String rememberMeKey;

	@Value("${security.rememberme.validity}")
	private Duration rememberMeValidity;

	public SecurityConfig(
			UrlRequestAuthenticationSuccessHandler urlSuccessHandler,
			RememberMeAuthenticationSuccessHandler rememberMeSuccessHandler,
			CustomAuthenticationFailureHandler failureHandler,
			ActionRegistry actionRegistry,
			Environment environment
	) {
		this.urlSuccessHandler = urlSuccessHandler;
		this.rememberMeSuccessHandler = rememberMeSuccessHandler;
		this.failureHandler = failureHandler;
		this.actionRegistry = actionRegistry;
		this.environment = environment;
	}

	// ---------------------------------------------------------------------
	// Core beans
	// ---------------------------------------------------------------------

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public WebSecurityCustomizer webSecurityCustomizer() {
		return web -> web.ignoring().requestMatchers(
				"/web/static/**",
				"/favicon.ico",
				"/robots.txt"
		);
	}

	// ---------------------------------------------------------------------
	// Security filter chain
	// ---------------------------------------------------------------------

	@Bean
	public SecurityFilterChain filterChain(
			HttpSecurity http,
			AuthenticationUserService userDetailsService
	) {

		boolean isProd = environment.acceptsProfiles(Profiles.of("prod"));

		http
				// -------------------------------------------------------------
				// Custom filters
				// -------------------------------------------------------------
				.addFilterBefore(
						rememberMeOldCookieErrorHandler(),
						RememberMeAuthenticationFilter.class
				)

				// -------------------------------------------------------------
				// Authorization rules
				// -------------------------------------------------------------
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/web/pub/**",
								"/api/pub/**"
						).permitAll()
						.requestMatchers("/web/sec/**")
						.access(roleAuthorizationManager())
						.anyRequest().authenticated()
				)

				// -------------------------------------------------------------
				// Form login
				// -------------------------------------------------------------
				.formLogin(form -> form
						.loginPage("/web/pub/login")
						.loginProcessingUrl("/web/pub/login")
						.usernameParameter("loginId")
						.passwordParameter("password")
						.successHandler(urlSuccessHandler)
						.failureHandler(failureHandler)
				)

				// -------------------------------------------------------------
				// Remember-me
				// -------------------------------------------------------------
				.rememberMe(remember -> remember
						.key(rememberMeKey)
						.rememberMeCookieName(REMEMBER_ME_COOKIE)
						.tokenValiditySeconds((int) rememberMeValidity.getSeconds())
						.useSecureCookie(isProd)
						.rememberMeParameter("remember-me")
						.userDetailsService(userDetailsService)
						.authenticationSuccessHandler(rememberMeSuccessHandler)
				)

				// -------------------------------------------------------------
				// Logout
				// -------------------------------------------------------------
				.logout(logout -> logout
						.logoutUrl("/web/sec/logout")
						.deleteCookies(REMEMBER_ME_COOKIE, "JSESSIONID")
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.logoutSuccessUrl("/web/pub/login")
				)

				// -------------------------------------------------------------
				// Exception handling (WEB vs API)
				// -------------------------------------------------------------
				.exceptionHandling(ex -> ex
						.accessDeniedPage("/web/pub/accessDenied")

						.defaultAuthenticationEntryPointFor(
								new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
								request -> request.getServletPath().startsWith("/api/")
						)

						.defaultAccessDeniedHandlerFor(
								(request, response, e) ->
										response.sendError(HttpServletResponse.SC_FORBIDDEN),
								request -> request.getServletPath().startsWith("/api/")
						)
				)

				// -------------------------------------------------------------
				// Session management
				// -------------------------------------------------------------
				.sessionManagement(session -> session
						.sessionFixation().migrateSession()
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				)

				// -------------------------------------------------------------
				// Security headers (production-aware)
				// -------------------------------------------------------------
				.headers(headers -> {
					headers
							.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
							.contentTypeOptions(withDefaults())
							.referrerPolicy(ref ->
									ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
							);

					if (isProd) {
						headers.httpStrictTransportSecurity(hsts -> hsts
								.includeSubDomains(true)
								.maxAgeInSeconds(Duration.ofDays(365).getSeconds())
						);
					}
				});

		return http.build();
	}

	// ---------------------------------------------------------------------
	// Authorization manager
	// ---------------------------------------------------------------------

	@Bean
	public AuthorizationManager<RequestAuthorizationContext> roleAuthorizationManager() {
		return new RoleBasedAccessDecisionManager(actionRegistry);
	}

	@Bean
	public RememberMeOldCookieErrorHandler rememberMeOldCookieErrorHandler() {
		return new RememberMeOldCookieErrorHandler();
	}
}

