package com.duckstore.duckstore.warehouse.dto;

import java.math.BigDecimal;

import com.duckstore.duckstore.warehouse.model.Duck;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Only price and quantity are editable; color and size are fixed once a duck exists. */
public record UpdateDuckRequest(
		@NotNull @Positive @Digits(integer = Duck.PRICE_INTEGER_DIGITS, fraction = Duck.PRICE_FRACTION_DIGITS) BigDecimal price,
		@NotNull @PositiveOrZero @Max(Duck.MAX_QUANTITY) Integer quantity) {
}
