package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sea: flat fee. Land: a fee per unit. Air: a fee per unit, with the air charge reduced by a percentage when the
 * order exceeds a unit threshold. The requirements set 400 USD, 10 USD per unit, 30 USD per unit and 15% above 1000
 * units; the values come from {@code pricing.shipping.*} in application.properties.
 * <p>
 * DECISIONS: "exceeds 1000" is strict, so exactly 1000 units pay the full air charge. The reduction applies
 * only to the air charge (not to the goods) and is shown as its own negative line in the breakdown. Shipping is
 * added after the percentage rules and is never itself subject to a percentage.
 */
public class ShippingRule implements PricingRule {

	private final BigDecimal seaFlatFee;
	private final BigDecimal landPerUnit;
	private final BigDecimal airPerUnit;
	private final int airBulkThreshold;
	private final int airBulkReductionPercent;

	public ShippingRule(BigDecimal seaFlatFee, BigDecimal landPerUnit, BigDecimal airPerUnit, int airBulkThreshold,
			int airBulkReductionPercent) {
		this.seaFlatFee = seaFlatFee;
		this.landPerUnit = landPerUnit;
		this.airPerUnit = airPerUnit;
		this.airBulkThreshold = airBulkThreshold;
		this.airBulkReductionPercent = airBulkReductionPercent;
	}

	@Override
	public List<Adjustment> apply(OrderDetails order, BigDecimal subtotal) {
		return switch (order.shippingMode()) {
			case Sea -> List.of(Adjustment.fixed("Sea shipping flat fee", seaFlatFee));
			case Land -> List.of(perUnit("Land shipping", landPerUnit, order.quantity()));
			case Air -> air(order.quantity());
		};
	}

	private List<Adjustment> air(int quantity) {
		Adjustment charge = perUnit("Air shipping", airPerUnit, quantity);
		if (quantity <= airBulkThreshold) {
			return List.of(charge);
		}
		Adjustment reduction = Adjustment.percentage("Air shipping reduction for more than " + airBulkThreshold
				+ " units", charge.amount(), -airBulkReductionPercent);
		return List.of(charge, reduction);
	}

	private static Adjustment perUnit(String label, BigDecimal perUnit, int quantity) {
		return Adjustment.fixed(label + " (" + Money.round(perUnit) + " per unit x " + quantity + ")",
				perUnit.multiply(BigDecimal.valueOf(quantity)));
	}
}
