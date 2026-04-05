package com.github.projectx.persistence;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;

/**
 * Persistence module bootstrap.
 * <p>
 * DataSource and transaction-manager auto configuration are excluded because
 * this module wires persistence contexts explicitly via
 * {@code PrimaryPersistenceContext}. This keeps bean names deterministic and
 * supports future multi-context expansion.
 */
@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
})
public class PersistenceApplication {
	/**
	 * Bootstraps the persistence module entry point so runtime wiring starts from a single deterministic initialization path.
	 */
	public static void main(String[] args) {
		SpringApplication.run(PersistenceApplication.class, args);
	}
}

