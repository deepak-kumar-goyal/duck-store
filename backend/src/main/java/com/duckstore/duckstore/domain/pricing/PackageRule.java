package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import com.duckstore.duckstore.domain.model.PackageType;

/**
 * Adds or removes a percentage per package type. The requirements set wood +5%, plastic +10% and cardboard -1%; the
 * values come from {@code pricing.package-percent.*} in application.properties (a negative percent is a discount).
 * <p>
 * DECISION: the percentage is taken from the subtotal after the volume discount, not from the raw quantity x price
 * (see PricingCalculator).
 */
public class PackageRule implements PricingRule {

	private final Map<PackageType, Integer> percentByPackage;

	/** @throws IllegalArgumentException when a package type has no percentage, so a bad configuration fails at startup */
	public PackageRule(Map<PackageType, Integer> percentByPackage) {
		EnumMap<PackageType, Integer> copy = new EnumMap<>(PackageType.class);
		copy.putAll(percentByPackage);
		if (!copy.keySet().containsAll(EnumSet.allOf(PackageType.class))) {
			throw new IllegalArgumentException("A percentage is required for every package type, got " + copy.keySet());
		}
		this.percentByPackage = copy;
	}

	@Override
	public List<Adjustment> apply(OrderDetails order, BigDecimal subtotal) {
		PackageType type = order.packageType();
		return List.of(Adjustment.percentage(type + " package", subtotal, percentByPackage.get(type)));
	}
}
