package com.duckstore.duckstore.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

	@Test
	void staleUpdateTellsTheClientToReloadAndRetry() {
		assertThat(handler.handleStaleUpdate().message()).contains("changed by someone else").contains("reload");
	}

	@Test
	void genericIntegrityViolationDoesNotBlameTheDuplicateKey() {
		assertThat(handler.handleIntegrity().message()).isEqualTo("The request conflicts with existing data");
	}
}
