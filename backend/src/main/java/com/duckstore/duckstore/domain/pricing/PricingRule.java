package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;
import java.util.List;

/** A single pricing rule. Returns the adjustments it applies, or an empty list when it does not apply. */
public interface PricingRule {

	/**
	 * @param subtotal the cost percentage rules are based on (quantity x price, after the volume discount)
	 */
	List<Adjustment> apply(OrderDetails order, BigDecimal subtotal);
}
