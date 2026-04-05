package com.github.projectx.persistence.config;

import com.github.projectx.persistence.repository.base.AbstractRepositoryImpl;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * Primary JPA persistence context configuration.
 * <p>
 * Bean names are sourced from {@link PersistenceContextNames} so services and
 * transactional boundaries remain decoupled from concrete config-class names.
 */
@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(RepositoryRuntimeProperties.class)
@EnableJpaAuditing
@EnableJpaRepositories(
		entityManagerFactoryRef = PersistenceContextNames.EM_FACTORY,
		transactionManagerRef = PersistenceContextNames.TX_MANAGER,
		basePackages = {"com.github.projectx.persistence.repository"},
		repositoryBaseClass = AbstractRepositoryImpl.class
)
public class PrimaryPersistenceContext {

	/**
	 * Keeps this public contract documented so callers rely on a single deterministic behavior at this layer.
	 */
	@Bean(name = PersistenceContextNames.DS_CONFIG)
	@ConfigurationProperties(prefix = "datasource.primary")
	public HikariConfig hikariConfig() {
		return new HikariConfig();
	}

	/**
	 * Builds the primary pooled datasource once so repository and Flyway access share the same
	 * connection lifecycle and tuning profile.
	 */
	@Primary
	@Bean(name = PersistenceContextNames.DATA_SOURCE, destroyMethod = "close")
	public HikariDataSource primaryDataSource(
			@Qualifier(PersistenceContextNames.DS_CONFIG) HikariConfig config,
			Environment environment
	) {
		ensureConfigured(config, environment);
		// HikariDataSource is AutoCloseable; close explicitly on bean shutdown.
		return new HikariDataSource(config);
	}

	/**
	 * Pins entity scanning to persistence module packages so startup is deterministic and unrelated
	 * classes are never attached to this persistence unit.
	 */
	@Primary
	@Bean(name = PersistenceContextNames.EM_FACTORY)
	public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(
			EntityManagerFactoryBuilder builder,
			@Qualifier(PersistenceContextNames.DATA_SOURCE) DataSource primaryDataSource
	) {
		return builder
				.dataSource(primaryDataSource)
				.packages(
						"com.github.projectx.persistence.entity",
						"com.github.projectx.persistence.view"
				)
				.persistenceUnit(PersistenceContextNames.PERSISTENCE_UNIT)
				.build();
	}

	/**
	 * Exposes a named transaction manager so service-layer transactions can target this persistence
	 * context explicitly when multiple data sources exist.
	 */
	@Primary
	@Bean(name = PersistenceContextNames.TX_MANAGER)
	public PlatformTransactionManager primaryTransactionManager(
			@Qualifier(PersistenceContextNames.EM_FACTORY) EntityManagerFactory primaryEntityManagerFactory
	) {
		return new JpaTransactionManager(primaryEntityManagerFactory);
	}

	private void ensureConfigured(HikariConfig config, Environment environment) {
		if (hasDescriptor(config)) {
			return;
		}
		String activeProfiles = String.join(",", environment.getActiveProfiles());
		throw new IllegalStateException(
				"Primary datasource is not configured. Define 'datasource.primary.jdbc-url' "
						+ "or 'datasource.primary.data-source-class-name'. Active profiles=[" + activeProfiles + "]"
		);
	}

	private boolean hasDescriptor(HikariConfig config) {
		return StringUtils.hasText(config.getJdbcUrl())
				|| StringUtils.hasText(config.getDataSourceClassName())
				|| config.getDataSource() != null;
	}
}

