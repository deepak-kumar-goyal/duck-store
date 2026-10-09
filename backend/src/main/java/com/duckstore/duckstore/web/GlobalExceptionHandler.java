package com.duckstore.duckstore.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ApiError handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return new ApiError("Validation failed", fieldErrors);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ApiError handleUnreadable() {
		return ApiError.of("Malformed request body or unsupported value (check color, size and shippingMode)");
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ApiError handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return ApiError.of("Invalid value for '" + ex.getName() + "'");
	}

	@ExceptionHandler(NotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	ApiError handleNotFound(NotFoundException ex) {
		return ApiError.of(ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	ApiError handleConflict(ConflictException ex) {
		return ApiError.of(ex.getMessage());
	}

	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	ApiError handleStaleUpdate() {
		return ApiError.of("The duck was changed by someone else in the meantime; reload and try again");
	}

	/** Safety net for races that the services' own checks cannot see. */
	@ExceptionHandler(DataIntegrityViolationException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	ApiError handleIntegrity() {
		return ApiError.of("The request conflicts with existing data");
	}
}
