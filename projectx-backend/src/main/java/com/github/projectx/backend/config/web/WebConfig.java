package com.github.projectx.backend.config.web;

import com.github.projectx.backend.common.thymeleaf.ThymeleafLayoutInterceptor;
import org.jspecify.annotations.NonNull;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	/* ------------------------------------------------------------------
	 * Thymeleaf
	 * ------------------------------------------------------------------ */

	@Bean
	public ClassLoaderTemplateResolver templateResolver() {
		ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
		resolver.setPrefix("templates/web/");
		resolver.setSuffix(".html");
		resolver.setTemplateMode("HTML");
		resolver.setCharacterEncoding("UTF-8");
		resolver.setCacheable(false);
		resolver.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return resolver;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new ThymeleafLayoutInterceptor())
				.addPathPatterns("/web/**")
				.excludePathPatterns(
						"/web/api/**",
						"/web/static/**"
				);
	}

	/* ------------------------------------------------------------------
	 * i18n
	 * ------------------------------------------------------------------ */

	@Bean
	public MessageSource messageSource() {
		ReloadableResourceBundleMessageSource ms = new ReloadableResourceBundleMessageSource();
		ms.setBasenames(
				"classpath:resourceBundles/messages",
				"classpath:resourceBundles/validation"
		);
		ms.setDefaultEncoding("UTF-8");
		ms.setCacheSeconds(60 * 60 * 24 * 365);
		return ms;
	}

	@Bean
	public LocaleResolver localeResolver() {
		return new AcceptHeaderLocaleResolver() {
			@Override
			public Locale resolveLocale(jakarta.servlet.http.@NonNull HttpServletRequest request) {
				String lang = request.getHeader("Language");
				if (lang == null || lang.isBlank()) {
					return Locale.ENGLISH;
				}
				String normalized = lang.trim().toLowerCase(Locale.ROOT);
				if (normalized.startsWith("mm") || normalized.startsWith("my")) {
					return Locale.of("mm");
				}
				return Locale.ENGLISH;
			}
		};
	}


	/* ------------------------------------------------------------------
	 * HTTP Client (Spring Boot 4 replacement for RestTemplate)
	 * ------------------------------------------------------------------ */

	@Bean
	public WebClient webClient() {
		HttpClient httpClient = HttpClient.create()
				.responseTimeout(Duration.ofSeconds(120));

		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}
}
