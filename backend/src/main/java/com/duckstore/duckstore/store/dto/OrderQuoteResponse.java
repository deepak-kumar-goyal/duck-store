package com.duckstore.duckstore.store.dto;

import java.math.BigDecimal;
import java.util.List;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Protection;
import com.duckstore.duckstore.domain.pricing.Adjustment;

/** {@code total} equals {@code baseCost} plus the sum of the {@code breakdown} amounts (negative = discount). */
public record OrderQuoteResponse(
		PackageType packageType,
		List<Protection> protections,
		BigDecimal unitPrice,
		BigDecimal baseCost,
		List<Adjustment> breakdown,
		BigDecimal total) {
}
