package com.devpulse.postgrestore;

import org.springframework.boot.SpringApplication;

public class TestPostgreStoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(PostgreStoreApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
