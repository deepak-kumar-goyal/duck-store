package com.duckstore.duckstore.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.duckstore.duckstore.IntegrationTest;
import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.ShippingMode;
import com.duckstore.duckstore.domain.pricing.OrderDetails;
import com.duckstore.duckstore.domain.pricing.PricingCalculator;

/**
 * Proves that application.properties holds exactly the numbers of the requirements: every rate is exercised by at
 * least one order, with totals worked out by hand from the requirements.
 */
class PricingConfigTest extends IntegrationTest {

	@Autowired
	PricingCalculator calculator;

	@Autowired
	PricingProperties properties;

	private BigDecimal total(int quantity, String unitPrice, String country, ShippingMode mode, PackageType type) {
		return calculator.calculate(new OrderDetails(quantity, new BigDecimal(unitPrice), country, mode, type))
				.total();
	}

	@Test
	void woodIndiaLand() {
		// 2000 + 5% wood (100) + 19% India (380) + 10 per unit land (100)
		assertThat(total(10, "200.00", "India", ShippingMode.Land, PackageType.Wood)).isEqualByComparingTo("2580.00");
	}

	@Test
	void cardboardUsaSeaWithVolumeDiscount() {
		// 1010 - 20% (202) = 808; -1% cardboard (8.08); +18% USA (145.44); +400 sea
		assertThat(total(101, "10.00", "USA", ShippingMode.Sea, PackageType.Cardboard))
				.isEqualByComparingTo("1345.36");
	}

	@Test
	void plasticOtherCountryAirWithBulkReduction() {
		// 1001 - 20% (200.20) = 800.80; +10% plastic (80.08); +15% other (120.12); air 30030 - 15% (4504.50)
		assertThat(total(1001, "1.00", "Canada", ShippingMode.Air, PackageType.Plastic))
				.isEqualByComparingTo("26526.50");
	}

	@Test
	void woodBoliviaLand() {
		// 100 + 5% wood (5) + 13% Bolivia (13) + 10 per unit land (10)
		assertThat(total(1, "100.00", "Bolivia", ShippingMode.Land, PackageType.Wood)).isEqualByComparingTo("128.00");
	}

	@Test
	void theThresholdsAreExactlyOneHundredAndOneThousandUnits() {
		assertThat(properties.volumeDiscount().threshold()).isEqualTo(100);
		assertThat(properties.shipping().airBulkThreshold()).isEqualTo(1000);
	}
}
