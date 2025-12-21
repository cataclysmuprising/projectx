package com.tamantaw.projectx.backend.config;

import com.tamantaw.projectx.backend.config.security.*;
import com.tamantaw.projectx.persistence.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.RememberMeAuthenticationFilter;

@Configuration
public class SecurityConfig {

	public static final String REMEMBER_ME_COOKIE = "projectx_backend_rbm";

	@Autowired
	private UrlRequestAuthenticationSuccessHandler urlRequestsuccessHandler;

	@Autowired
	private RememberMeAuthenticationSuccessHandler rememberMeSuccessHandler;

	@Autowired
	private CustomAuthenticationFailureHandler failureHandler;

	@Autowired
	private RoleService roleService;

	@Bean
	public static PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	//@formatter:off
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationUserService userDetailsService) throws Exception {
		http
				.addFilterBefore(new RememberMeOldCookieErrorHandler(), RememberMeAuthenticationFilter.class)

				.authorizeHttpRequests((authorize) ->
						authorize.requestMatchers(
										"/login*"
										,"/accessDenied"
										,"/error/*"
										,"/static/**"
										,"/pub/**"
										,"/api/pub/**").permitAll()
								.requestMatchers("/sec/**")
								.access(new RoleBasedAccessDecisionManager(roleService))
								.anyRequest().authenticated()
				)
				.formLogin(
						form -> form
								.loginPage("/login")
								.loginProcessingUrl("/login")
								.usernameParameter("loginId")
								.passwordParameter("password")
								.failureHandler(failureHandler)
								.successHandler(urlRequestsuccessHandler)
				)
				.exceptionHandling(
						exceptionHandler -> exceptionHandler
								.accessDeniedPage("/accessDenied")
				)
				.sessionManagement(
						session -> session
								.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
								.sessionFixation().migrateSession()
				)
				.rememberMe(
						remember -> remember
								.key("cBZ1c2VyIiwic2NvcGUiOlsiYmFya2VuZCIsIkJlY5QiLCJ3cmG0ZSIsInVwZG") // hash-key
								.rememberMeCookieName(REMEMBER_ME_COOKIE)
								.tokenValiditySeconds(4*604800) // 1 week , let the default 4 week
								.rememberMeParameter("remember-me")
								.userDetailsService(userDetailsService)
								.authenticationSuccessHandler(rememberMeSuccessHandler)
				)
				.logout(
						logout -> logout
								.logoutUrl("/logout")
								.deleteCookies(REMEMBER_ME_COOKIE,"JSESSIONID")
								.clearAuthentication(true)
								.invalidateHttpSession(true)
								.logoutSuccessUrl("/login")
				)
		;
		return http.build();
	}
	//@formatter:on
}
