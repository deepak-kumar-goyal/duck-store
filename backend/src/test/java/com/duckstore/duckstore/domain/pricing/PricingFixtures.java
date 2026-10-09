package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;
import java.util.Map;

import com.duckstore.duckstore.domain.model.PackageType;

/**
 * The numbers written in the requirements. The unit tests use these instead of application.properties on purpose, so
 * they prove what the rules do for the specified values; PricingConfigTest proves that application.properties holds
 * the same values.
 */
final class PricingFixtures {

	private PricingFixtures() {
	}

	static VolumeDiscountRule volumeDiscountRule() {
		return new VolumeDiscountRule(100, 20);
	}

	static PackageRule packageRule() {
		return new PackageRule(Map.of(PackageType.Wood, 5, PackageType.Plastic, 10, PackageType.Cardboard, -1));
	}

	static DestinationRule destinationRule() {
		return new DestinationRule(Map.of("USA", 18, "Bolivia", 13, "India", 19), 15);
	}

	static ShippingRule shippingRule() {
		return new ShippingRule(new BigDecimal("400"), BigDecimal.TEN, new BigDecimal("30"), 1000, 15);
	}

	static PricingCalculator calculator() {
		return PricingCalculator.of(volumeDiscountRule(), packageRule(), destinationRule(), shippingRule());
	}
}
