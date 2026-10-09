package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies the pricing rules in a fixed order: the volume discount first, then the package, destination and
 * shipping rules. Percentage rules are all based on the same subtotal (cost after the volume discount) and do not
 * compound; shipping charges are added on top.
 * <p>
 * DECISION: the requirements say "% of the total cost" for every rule but never say which total, or whether
 * percentages build on each other. Chosen reading: "total cost" is quantity x price after the volume discount,
 * and package and destination percentages are each taken from that same figure. Compounding would make the result
 * depend on the order of the rules, which the requirements do not define.
 * <p>
 * DECISION: every line is rounded to 2 decimals (HALF_UP) and the total is the exact sum of the lines, so the
 * itemized breakdown always adds up to the cent.
 */
public class PricingCalculator {

	private final PricingRule volumeDiscount;
	private final List<PricingRule> chargeRules;

	PricingCalculator(PricingRule volumeDiscount, List<PricingRule> chargeRules) {
		this.volumeDiscount = volumeDiscount;
		this.chargeRules = chargeRules;
	}

	/** The order of the rules is fixed here; the numbers they use are supplied by the caller. */
	public static PricingCalculator of(VolumeDiscountRule volumeDiscount, PackageRule packageRule,
			DestinationRule destinationRule, ShippingRule shippingRule) {
		return new PricingCalculator(volumeDiscount, List.of(packageRule, destinationRule, shippingRule));
	}

	public PriceBreakdown calculate(OrderDetails order) {
		BigDecimal baseCost = order.baseCost();
		List<Adjustment> adjustments = new ArrayList<>(volumeDiscount.apply(order, baseCost));
		BigDecimal subtotal = baseCost.add(sum(adjustments));
		for (PricingRule rule : chargeRules) {
			adjustments.addAll(rule.apply(order, subtotal));
		}
		return new PriceBreakdown(baseCost, List.copyOf(adjustments), baseCost.add(sum(adjustments)));
	}

	private static BigDecimal sum(List<Adjustment> adjustments) {
		return adjustments.stream().map(Adjustment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
