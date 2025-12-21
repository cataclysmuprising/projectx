package com.tamantaw.projectx.backend.config;

import com.tamantaw.projectx.backend.common.thymeleaf.ThymeleafLayoutInterceptor;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.Ordered;
import org.springframework.http.CacheControl;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

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

	@Bean
	public MappedInterceptor thymeleafInterceptor() {
		return new MappedInterceptor(null, new ThymeleafLayoutInterceptor());
	}

	/* ------------------------------------------------------------------
	 * Static resources
	 * ------------------------------------------------------------------ */

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**")
				.addResourceLocations("classpath:/static/")
				.setCacheControl(CacheControl.maxAge(Duration.ofDays(365)));
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


	/* ------------------------------------------------------------------
	 * HTTP Client (Spring Boot 4 replacement for RestTemplate)
	 * ------------------------------------------------------------------ */

	@Bean
	public WebClient webClient() {
		HttpClient httpClient = HttpClient.create()
				.responseTimeout(Duration.ofSeconds(60));

		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}
}
