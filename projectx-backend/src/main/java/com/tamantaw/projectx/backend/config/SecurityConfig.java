package com.tamantaw.projectx.backend.config;

import com.tamantaw.projectx.backend.config.security.*;
import com.tamantaw.projectx.backend.utils.ActionRegistry;
import com.tamantaw.projectx.persistence.service.RoleService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	public static final String REMEMBER_ME_COOKIE = "projectx_backend_rbm";

	private final UrlRequestAuthenticationSuccessHandler urlSuccessHandler;
	private final RememberMeAuthenticationSuccessHandler rememberMeSuccessHandler;
	private final CustomAuthenticationFailureHandler failureHandler;
	private final RoleService roleService;
	private final ActionRegistry actionRegistry;

	public SecurityConfig(
			UrlRequestAuthenticationSuccessHandler urlSuccessHandler,
			RememberMeAuthenticationSuccessHandler rememberMeSuccessHandler,
			CustomAuthenticationFailureHandler failureHandler,
			RoleService roleService,
			ActionRegistry actionRegistry
	) {
		this.urlSuccessHandler = urlSuccessHandler;
		this.rememberMeSuccessHandler = rememberMeSuccessHandler;
		this.failureHandler = failureHandler;
		this.roleService = roleService;
		this.actionRegistry = actionRegistry;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain filterChain(
			HttpSecurity http,
			AuthenticationUserService userDetailsService
	) {

		http
				.addFilterBefore(
						new RememberMeOldCookieErrorHandler(),
						RememberMeAuthenticationFilter.class
				)

				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/web/pub/**",
								"/web/static/**",
								"/api/pub/**"
						).permitAll()
						.requestMatchers("/web/sec/**")
						.access(new RoleBasedAccessDecisionManager(roleService, actionRegistry))
						.anyRequest().authenticated()
				)

				.formLogin(form -> form
						.loginPage("/web/pub/login")
						.loginProcessingUrl("/web/pub/login")
						.usernameParameter("loginId")
						.passwordParameter("password")
						.successHandler(urlSuccessHandler)
						.failureHandler(failureHandler)
				)

				.rememberMe(remember -> remember
						.key("cBZ1c2VyIiwic2NvcGUiOlsiYmFya2VuZCIsIkJlY5QiLCJ3cmG0ZSIsInVwZG")
						.rememberMeCookieName(REMEMBER_ME_COOKIE)
						.tokenValiditySeconds(4 * 604800)
						.rememberMeParameter("remember-me")
						.userDetailsService(userDetailsService)
						.authenticationSuccessHandler(rememberMeSuccessHandler)
				)

				.logout(logout -> logout
						.logoutUrl("/web/sec/logout")
						.deleteCookies(REMEMBER_ME_COOKIE, "JSESSIONID")
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.logoutSuccessUrl("/web/pub/login")
				)

				.exceptionHandling(ex -> ex
						.accessDeniedPage("/web/pub/accessDenied")
				)

				.sessionManagement(session -> session
						.sessionFixation().migrateSession()
						.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				);

		return http.build();
	}
}

