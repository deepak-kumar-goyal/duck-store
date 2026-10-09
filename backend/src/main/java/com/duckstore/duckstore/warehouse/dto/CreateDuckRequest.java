package com.duckstore.duckstore.warehouse.dto;

import java.math.BigDecimal;

import com.duckstore.duckstore.domain.model.Color;
import com.duckstore.duckstore.domain.model.Size;
import com.duckstore.duckstore.warehouse.model.Duck;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateDuckRequest(
		@NotNull Color color,
		@NotNull Size size,
		@NotNull @Positive @Digits(integer = Duck.PRICE_INTEGER_DIGITS, fraction = Duck.PRICE_FRACTION_DIGITS) BigDecimal price,
		@NotNull @Positive @Max(Duck.MAX_QUANTITY) Integer quantity) {
}
