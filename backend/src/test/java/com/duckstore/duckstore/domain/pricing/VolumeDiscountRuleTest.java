package com.duckstore.duckstore.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.ShippingMode;

class VolumeDiscountRuleTest {

	private final VolumeDiscountRule rule = PricingFixtures.volumeDiscountRule();

	private List<Adjustment> applyFor(int quantity) {
		OrderDetails order = new OrderDetails(quantity, new BigDecimal("10.00"), "India", ShippingMode.Land,
				PackageType.Wood);
		return rule.apply(order, order.baseCost());
	}

	@Test
	void exactlyOneHundredUnitsGetNoDiscount() {
		assertThat(applyFor(100)).isEmpty();
	}

	@Test
	void moreThanOneHundredUnitsGetTwentyPercentOffTheTotalCost() {
		List<Adjustment> adjustments = applyFor(101);

		assertThat(adjustments).hasSize(1);
		assertThat(adjustments.get(0).amount()).isEqualByComparingTo("-202.00");
		assertThat(adjustments.get(0).description()).contains("-20%");
	}
}
