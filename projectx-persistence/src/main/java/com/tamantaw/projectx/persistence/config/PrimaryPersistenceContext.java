package com.tamantaw.projectx.persistence.config;

import com.tamantaw.projectx.persistence.repository.base.AbstractRepositoryImpl;
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
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties
@EnableJpaAuditing
@EnableJpaRepositories(entityManagerFactoryRef = PrimaryPersistenceContext.EM_FACTORY, transactionManagerRef = PrimaryPersistenceContext.TX_MANAGER, basePackages = {"com.tamantaw.projectx.persistence"}, repositoryBaseClass = AbstractRepositoryImpl.class)
public class PrimaryPersistenceContext {
	public static final String EM_FACTORY = "primaryEntityManagerFactory";
	public static final String TX_MANAGER = "primaryTransactionManager";
	private static final String DS_CONFIG = "primaryDSConfig";
	private static final String DATASOURCE = "primaryDataSource";

	@Bean(name = DS_CONFIG)
	@ConfigurationProperties(prefix = "datasource.primary")
	public HikariConfig hikariConfig() {
		return new HikariConfig();
	}

	@Primary
	@Bean(name = DATASOURCE, destroyMethod = "close")
	public HikariDataSource primaryDataSource(@Qualifier(DS_CONFIG) HikariConfig config) {
		return new HikariDataSource(config);
	}

	@Primary
	@Bean(name = EM_FACTORY)
	public LocalContainerEntityManagerFactoryBean primaryEntityManagerFactory(EntityManagerFactoryBuilder builder, @Qualifier(DATASOURCE) DataSource primaryDataSource) {
		return builder.dataSource(primaryDataSource).packages("com.tamantaw.projectx.persistence.entity", "com.tamantaw.projectx.persistence.view").persistenceUnit("primaryPSTUnit").build();
	}

	@Primary
	@Bean(name = TX_MANAGER)
	public PlatformTransactionManager primaryTransactionManager(@Qualifier(EM_FACTORY) EntityManagerFactory primaryEntityManagerFactory) {
		return new JpaTransactionManager(primaryEntityManagerFactory);
	}
}
