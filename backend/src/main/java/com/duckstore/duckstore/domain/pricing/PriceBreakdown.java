package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;
import java.util.List;

/** {@code total} is always {@code baseCost} plus the sum of all {@code adjustments}. */
public record PriceBreakdown(BigDecimal baseCost, List<Adjustment> adjustments, BigDecimal total) {
}
