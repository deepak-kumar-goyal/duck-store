package com.duckstore.duckstore.domain.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.ShippingMode;

class PricingCalculatorTest {

	private final PricingCalculator calculator = PricingFixtures.calculator();

	private PriceBreakdown price(int quantity, String unitPrice, String country, ShippingMode mode, PackageType type) {
		return calculator.calculate(new OrderDetails(quantity, new BigDecimal(unitPrice), country, mode, type));
	}

	@Test
	void baseCostIsQuantityTimesPrice() {
		PriceBreakdown result = price(3, "19.99", "India", ShippingMode.Land, PackageType.Wood);

		assertThat(result.baseCost()).isEqualByComparingTo("59.97");
	}

	@Test
	void smallWoodOrderToIndiaByLand() {
		PriceBreakdown result = price(10, "200.00", "India", ShippingMode.Land, PackageType.Wood);

		// 2000 + 5% wood (100) + 19% India (380) + 10 per unit land (100)
		assertThat(result.total()).isEqualByComparingTo("2580.00");
		assertThat(result.adjustments()).hasSize(3);
	}

	@Test
	void largeCardboardOrderToUsaBySeaAppliesDiscountOnTheDiscountedSubtotal() {
		PriceBreakdown result = price(101, "10.00", "USA", ShippingMode.Sea, PackageType.Cardboard);

		// 1010 - 20% (202) = 808; -1% cardboard (8.08); +18% USA (145.44); +400 sea
		assertThat(result.total()).isEqualByComparingTo("1345.36");
	}

	@Test
	void hugeAirOrderToOtherCountryWithPlasticPackage() {
		PriceBreakdown result = price(1001, "1.00", "Canada", ShippingMode.Air, PackageType.Plastic);

		// 1001 - 20% (200.20) = 800.80; +10% plastic (80.08); +15% other (120.12); air 30030 - 15% (4504.50)
		assertThat(result.total()).isEqualByComparingTo("26526.50");
	}

	private static List<BigDecimal> amounts(PriceBreakdown result) {
		return result.adjustments().stream().map(Adjustment::amount).toList();
	}

	@Test
	void woodAndDestinationPercentagesAreTakenFromTheDiscountedSubtotalAndDoNotCompound() {
		PriceBreakdown result = price(200, "10.00", "USA", ShippingMode.Land, PackageType.Wood);

		// base 2000; volume -20% = -400 -> subtotal 1600; wood +5% of 1600 = 80; USA +18% of 1600 = 288 (not of
		// 1680 = 302.40, and not of the undiscounted 2000 = 360); land 200 x 10 = 2000
		assertThat(amounts(result)).usingElementComparator(BigDecimal::compareTo).containsExactly(
				new BigDecimal("-400.00"), new BigDecimal("80.00"), new BigDecimal("288.00"),
				new BigDecimal("2000.00"));
		assertThat(result.total()).isEqualByComparingTo("3968.00");
	}

	@Test
	void plasticAndDestinationPercentagesAreTakenFromTheDiscountedSubtotalAndDoNotCompound() {
		PriceBreakdown result = price(150, "4.00", "Bolivia", ShippingMode.Air, PackageType.Plastic);

		// base 600; volume -20% = -120 -> subtotal 480; plastic +10% of 480 = 48; Bolivia +13% of 480 = 62.40;
		// air 150 x 30 = 4500 (no air reduction up to 1000 units)
		assertThat(amounts(result)).usingElementComparator(BigDecimal::compareTo).containsExactly(
				new BigDecimal("-120.00"), new BigDecimal("48.00"), new BigDecimal("62.40"),
				new BigDecimal("4500.00"));
		assertThat(result.total()).isEqualByComparingTo("5090.40");
	}

	@Test
	void volumeDiscountStartsAtOneHundredAndOneUnits() {
		PriceBreakdown atTheLimit = price(100, "1.00", "India", ShippingMode.Land, PackageType.Wood);
		PriceBreakdown overTheLimit = price(101, "1.00", "India", ShippingMode.Land, PackageType.Wood);

		// 100 + 5 (wood) + 19 (India) + 1000 (land), no discount line at all
		assertThat(atTheLimit.adjustments()).hasSize(3);
		assertThat(atTheLimit.total()).isEqualByComparingTo("1124.00");
		// 101 - 20.20 = 80.80; wood 4.04; India 15.352 -> 15.35; land 1010
		assertThat(overTheLimit.adjustments()).hasSize(4);
		assertThat(overTheLimit.total()).isEqualByComparingTo("1110.19");
	}

	@Test
	void airChargeIsNotReducedAtExactlyOneThousandUnitsButIsAtOneThousandAndOne() {
		PriceBreakdown atTheLimit = price(1000, "1.00", "Canada", ShippingMode.Air, PackageType.Plastic);
		PriceBreakdown overTheLimit = price(1001, "1.00", "Canada", ShippingMode.Air, PackageType.Plastic);

		// 1000 - 200 = 800; plastic +80; other +120; air 30000 in full: no reduction line
		assertThat(atTheLimit.adjustments()).hasSize(4);
		assertThat(atTheLimit.total()).isEqualByComparingTo("31000.00");
		// the same order one unit larger gets a separate negative air line of 15% of 30030
		assertThat(overTheLimit.adjustments()).hasSize(5);
		assertThat(overTheLimit.adjustments().get(4).amount()).isEqualByComparingTo("-4504.50");
	}

	@Test
	void totalIsAlwaysBaseCostPlusEveryListedAdjustment() {
		PriceBreakdown result = price(333, "7.77", "Bolivia", ShippingMode.Air, PackageType.Cardboard);

		BigDecimal sum = result.adjustments().stream().map(Adjustment::amount).reduce(BigDecimal.ZERO,
				BigDecimal::add);
		assertThat(result.total()).isEqualByComparingTo(result.baseCost().add(sum));
		assertThat(result.total().scale()).isEqualTo(2);
	}
}
