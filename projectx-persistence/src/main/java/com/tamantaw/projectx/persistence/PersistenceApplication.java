package com.tamantaw.projectx.persistence;

import org.springframework.boot.SpringApplication;import org.springframework.boot.autoconfigure.SpringBootApplication;import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;import org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration;import org.springframework.context.annotation.ComponentScan;//@formatter:off
@SpringBootApplication(exclude = {
		DataSourceAutoConfiguration.class,
		DataSourceTransactionManagerAutoConfiguration.class,
		})
//@formatter:on

public class PersistenceApplication {
	public static void main(String[] args) {
		SpringApplication.run(PersistenceApplication.class, args);
	}
}
