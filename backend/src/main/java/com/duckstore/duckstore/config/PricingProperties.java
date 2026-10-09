package com.duckstore.duckstore.config;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import com.duckstore.duckstore.domain.model.PackageType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Every number used by the pricing rules, bound from the {@code pricing.*} keys of application.properties. The
 * domain package stays free of Spring: these values are handed to the rules by {@link PricingConfig}. A missing or
 * invalid value stops the application at startup instead of silently pricing with zero.
 */
@Validated
@ConfigurationProperties(prefix = "pricing")
public record PricingProperties(
		@Valid @NotNull VolumeDiscount volumeDiscount,
		@NotEmpty Map<PackageType, Integer> packagePercent,
		@Valid @NotNull Destination destination,
		@Valid @NotNull Shipping shipping) {

	public record VolumeDiscount(
			@NotNull @PositiveOrZero Integer threshold,
			@NotNull @PositiveOrZero Integer percent) {
	}

	public record Destination(
			@NotNull @PositiveOrZero Integer defaultPercent,
			@NotNull Map<String, Integer> percent) {
	}

	public record Shipping(
			@NotNull @PositiveOrZero BigDecimal seaFlatFee,
			@NotNull @PositiveOrZero BigDecimal landPerUnit,
			@NotNull @PositiveOrZero BigDecimal airPerUnit,
			@NotNull @PositiveOrZero Integer airBulkThreshold,
			@NotNull @PositiveOrZero Integer airBulkReductionPercent) {
	}
}
