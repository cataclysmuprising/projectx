package com.github.projectx.persistence.config;

/**
 * Central bean names for the default persistence context.
 *
 * <p>
 * This keeps service/repository code decoupled from a specific
 * configuration class name (e.g. PrimaryPersistenceContext) and
 * allows additional persistence contexts/transaction managers to
 * coexist with explicit names.
 * </p>
 */
public final class PersistenceContextNames {

	public static final String EM_FACTORY = "persistenceEntityManagerFactory";
	public static final String TX_MANAGER = "persistenceTransactionManager";
	public static final String DATA_SOURCE = "persistenceDataSource";
	public static final String DS_CONFIG = "persistenceDSConfig";
	public static final String PERSISTENCE_UNIT = "persistencePSTUnit";

	private PersistenceContextNames() {
	}
}
