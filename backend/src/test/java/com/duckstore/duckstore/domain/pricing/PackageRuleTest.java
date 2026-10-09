package com.duckstore.duckstore.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.ShippingMode;

class PackageRuleTest {

	private final PackageRule rule = PricingFixtures.packageRule();

	private BigDecimal amountFor(PackageType type, String subtotal) {
		OrderDetails order = new OrderDetails(1, new BigDecimal(subtotal), "India", ShippingMode.Land, type);
		return rule.apply(order, new BigDecimal(subtotal)).get(0).amount();
	}

	@ParameterizedTest
	@CsvSource({ "Wood,5.00", "Plastic,10.00", "Cardboard,-1.00" })
	void adjustsTheSubtotalByThePackagePercentage(PackageType type, String expected) {
		assertThat(amountFor(type, "100.00")).isEqualByComparingTo(expected);
	}

	@Test
	void everyPackageTypeMustHaveAPercentage() {
		assertThatThrownBy(() -> new PackageRule(Map.of(PackageType.Wood, 5)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("every package type");
	}

	@Test
	void roundsHalfUpToTheCent() {
		assertThat(amountFor(PackageType.Wood, "10.10")).isEqualByComparingTo("0.51");
		assertThat(amountFor(PackageType.Cardboard, "10.50")).isEqualByComparingTo("-0.11");
	}
}
