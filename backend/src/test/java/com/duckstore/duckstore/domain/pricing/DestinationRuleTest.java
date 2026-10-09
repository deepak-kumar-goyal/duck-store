package com.duckstore.duckstore.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.ShippingMode;

class DestinationRuleTest {

	private final DestinationRule rule = PricingFixtures.destinationRule();

	@ParameterizedTest
	@CsvSource({
			"USA,18.00",
			"Bolivia,13.00",
			"India,19.00",
			"Canada,15.00",
			"usa,18.00",
			"  INDIA ,19.00" })
	void addsThePercentageForTheDestination(String country, String expected) {
		OrderDetails order = new OrderDetails(1, new BigDecimal("100.00"), country, ShippingMode.Land,
				PackageType.Wood);

		Adjustment adjustment = rule.apply(order, new BigDecimal("100.00")).get(0);

		assertThat(adjustment.amount()).isEqualByComparingTo(expected);
	}

	@Test
	void configuredCountryNamesMatchWhateverCaseTheyAreWrittenIn() {
		// application.properties holds the keys in lower case
		DestinationRule fromProperties = new DestinationRule(Map.of("usa", 18), 15);
		OrderDetails order = new OrderDetails(1, new BigDecimal("100.00"), "USA", ShippingMode.Land,
				PackageType.Wood);

		assertThat(fromProperties.apply(order, new BigDecimal("100.00")).get(0).amount())
				.isEqualByComparingTo("18.00");
	}

	@Test
	void unlistedCountriesGetTheConfiguredDefault() {
		DestinationRule custom = new DestinationRule(Map.of("USA", 18), 7);
		OrderDetails order = new OrderDetails(1, new BigDecimal("100.00"), "Peru", ShippingMode.Land,
				PackageType.Wood);

		assertThat(custom.apply(order, new BigDecimal("100.00")).get(0).amount()).isEqualByComparingTo("7.00");
	}
}
