package com.github.projectx.persistence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Repository runtime tuning shared by the QueryDSL base repository.
 * Invalid values are sanitized in the repository layer so operational overrides fail closed.
 */
@ConfigurationProperties(prefix = "projectx.repository")
public class RepositoryRuntimeProperties {

	public static final int DEFAULT_BULK_IN_CHUNK_SIZE = 1000;
	public static final int DEFAULT_FETCH_IN_CHUNK_SIZE = 1000;
	public static final int DEFAULT_MAX_CASE_ORDER_IDS = 200;
	public static final int DEFAULT_GRAPH_CACHE_MAX_SIZE = 2000;
	public static final String DEFAULT_POSTGRES_DB = "auto";

	private int bulkInChunkSize = DEFAULT_BULK_IN_CHUNK_SIZE;
	private int fetchInChunkSize = DEFAULT_FETCH_IN_CHUNK_SIZE;
	private int maxCaseOrderIds = DEFAULT_MAX_CASE_ORDER_IDS;
	private int graphCacheMaxSize = DEFAULT_GRAPH_CACHE_MAX_SIZE;
	private String postgresDb = DEFAULT_POSTGRES_DB;

	public int getBulkInChunkSize() {
		return bulkInChunkSize;
	}

	public void setBulkInChunkSize(int bulkInChunkSize) {
		this.bulkInChunkSize = bulkInChunkSize;
	}

	public int getFetchInChunkSize() {
		return fetchInChunkSize;
	}

	public void setFetchInChunkSize(int fetchInChunkSize) {
		this.fetchInChunkSize = fetchInChunkSize;
	}

	public int getMaxCaseOrderIds() {
		return maxCaseOrderIds;
	}

	public void setMaxCaseOrderIds(int maxCaseOrderIds) {
		this.maxCaseOrderIds = maxCaseOrderIds;
	}

	public int getGraphCacheMaxSize() {
		return graphCacheMaxSize;
	}

	public void setGraphCacheMaxSize(int graphCacheMaxSize) {
		this.graphCacheMaxSize = graphCacheMaxSize;
	}

	public String getPostgresDb() {
		return postgresDb;
	}

	public void setPostgresDb(String postgresDb) {
		this.postgresDb = postgresDb;
	}
}
