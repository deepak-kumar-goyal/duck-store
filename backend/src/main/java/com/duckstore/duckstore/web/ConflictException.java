package com.duckstore.duckstore.web;

/** Thrown when a request conflicts with the current state; mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
