package com.duckstore.duckstore.store.dto;

import com.duckstore.duckstore.domain.model.Color;
import com.duckstore.duckstore.domain.model.ShippingMode;
import com.duckstore.duckstore.domain.model.Size;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * An order carries no price: it is resolved from the warehouse (see OrderService).
 * <p>
 * DECISION: the country is free text because the requirements only name three special destinations and treat
 * everything else alike; it is matched case-insensitively and trimmed (see DestinationRule).
 * <p>
 * FUTURE SCOPE: the country could be taken from a constant list (for example ISO country codes or a countries
 * table) and validated here, so that a typo is rejected with a 400 instead of silently priced as "other".
 */
public record OrderRequest(
		@NotNull Color color,
		@NotNull Size size,
		@NotNull @Positive Integer quantity,
		@NotBlank String country,
		@NotNull ShippingMode shippingMode) {
}
