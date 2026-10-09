package com.duckstore.duckstore.web;

import java.util.Map;

/** Error body returned by every failing endpoint. {@code fieldErrors} is only filled for validation failures. */
public record ApiError(String message, Map<String, String> fieldErrors) {

	public static ApiError of(String message) {
		return new ApiError(message, Map.of());
	}
}
