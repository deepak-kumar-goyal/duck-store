package com.duckstore.duckstore.web;

/** Every URL the API exposes, in one place. */
public final class ApiPaths {

	public static final String DUCKS = "/api/ducks";
	public static final String ORDERS = "/api/orders";

	/** Relative to {@link #ORDERS}. */
	public static final String QUOTE = "/quote";

	/** Relative to {@link #DUCKS}. */
	public static final String BY_ID = "/{id}";

	private ApiPaths() {
	}
}
