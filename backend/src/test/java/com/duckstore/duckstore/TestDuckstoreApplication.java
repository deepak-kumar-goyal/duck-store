package com.duckstore.duckstore;

import org.springframework.boot.SpringApplication;

public class TestDuckstoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(DuckstoreApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
