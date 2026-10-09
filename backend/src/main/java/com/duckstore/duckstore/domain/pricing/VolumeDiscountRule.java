package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * More than {@code threshold} units: a {@code discountPercent} discount on the total cost (quantity x price). The
 * requirements set 100 units and 20%; the values come from {@code pricing.volume-discount.*} in
 * application.properties.
 * <p>
 * DECISION: "more than 100" is strict, so exactly 100 units get no discount.
 */
public class VolumeDiscountRule implements PricingRule {

	private final int threshold;
	private final int discountPercent;

	public VolumeDiscountRule(int threshold, int discountPercent) {
		this.threshold = threshold;
		this.discountPercent = discountPercent;
	}

	@Override
	public List<Adjustment> apply(OrderDetails order, BigDecimal subtotal) {
		if (order.quantity() <= threshold) {
			return List.of();
		}
		return List.of(Adjustment.percentage("Volume discount for more than " + threshold + " units",
				order.baseCost(), -discountPercent));
	}
}
