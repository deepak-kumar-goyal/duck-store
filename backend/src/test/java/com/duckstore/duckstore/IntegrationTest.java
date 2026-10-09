package com.duckstore.duckstore;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import com.duckstore.duckstore.warehouse.repository.DuckRepository;

/** Base for tests that need the real MySQL (Testcontainers) with the Flyway schema applied. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
public abstract class IntegrationTest {

	@Autowired
	protected DuckRepository duckRepository;

	@BeforeEach
	void cleanDatabase() {
		duckRepository.deleteAll();
	}
}
