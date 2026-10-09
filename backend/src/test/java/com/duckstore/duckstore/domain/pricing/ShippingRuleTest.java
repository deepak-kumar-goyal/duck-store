package com.duckstore.duckstore.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.ShippingMode;

class ShippingRuleTest {

	private final ShippingRule rule = PricingFixtures.shippingRule();

	private List<Adjustment> shippingFor(ShippingMode mode, int quantity) {
		OrderDetails order = new OrderDetails(quantity, BigDecimal.ONE, "India", mode, PackageType.Wood);
		return rule.apply(order, order.baseCost());
	}

	@Test
	void seaAddsAFlatFourHundredRegardlessOfQuantity() {
		assertThat(shippingFor(ShippingMode.Sea, 1)).extracting(Adjustment::amount)
				.usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
				.containsExactly(new BigDecimal("400.00"));
		assertThat(shippingFor(ShippingMode.Sea, 5000)).extracting(Adjustment::amount)
				.usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
				.containsExactly(new BigDecimal("400.00"));
	}

	@Test
	void landAddsTenPerUnit() {
		List<Adjustment> adjustments = shippingFor(ShippingMode.Land, 7);

		assertThat(adjustments).hasSize(1);
		assertThat(adjustments.get(0).amount()).isEqualByComparingTo("70.00");
	}

	@Test
	void airAddsThirtyPerUnitUpToOneThousandUnits() {
		List<Adjustment> adjustments = shippingFor(ShippingMode.Air, 1000);

		assertThat(adjustments).hasSize(1);
		assertThat(adjustments.get(0).amount()).isEqualByComparingTo("30000.00");
	}

	@Test
	void airChargeIsReducedByFifteenPercentWhenOrderExceedsOneThousandUnits() {
		List<Adjustment> adjustments = shippingFor(ShippingMode.Air, 1001);

		assertThat(adjustments).hasSize(2);
		assertThat(adjustments.get(0).amount()).isEqualByComparingTo("30030.00");
		assertThat(adjustments.get(1).amount()).isEqualByComparingTo("-4504.50");
	}
}
