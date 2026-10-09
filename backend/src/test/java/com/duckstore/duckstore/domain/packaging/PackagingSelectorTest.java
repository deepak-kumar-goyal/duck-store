package com.duckstore.duckstore.domain.packaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Protection;
import com.duckstore.duckstore.domain.model.ShippingMode;
import com.duckstore.duckstore.domain.model.Size;
import com.duckstore.duckstore.domain.packaging.packagingstrategy.CardboardPackage;
import com.duckstore.duckstore.domain.packaging.packagingstrategy.PlasticPackage;
import com.duckstore.duckstore.domain.packaging.packagingstrategy.WoodPackage;
import com.duckstore.duckstore.domain.packaging.protectionstrategy.AirProtection;
import com.duckstore.duckstore.domain.packaging.protectionstrategy.LandProtection;
import com.duckstore.duckstore.domain.packaging.protectionstrategy.SeaProtection;

class PackagingSelectorTest {

	private final PackagingSelector selector = PackagingSelector.standard();

	@ParameterizedTest
	@CsvSource({ "XLarge,Wood", "Large,Wood", "Medium,Cardboard", "Small,Plastic", "XSmall,Plastic" })
	void packageDependsOnSize(Size size, PackageType expected) {
		assertThat(selector.choose(size, ShippingMode.Land).packageType()).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({ "XLarge", "Large", "Medium" })
	void airWithWoodOrCardboardUsesPolystyreneBalls(Size size) {
		assertThat(selector.choose(size, ShippingMode.Air).protections())
				.containsExactly(Protection.PolystyreneBalls);
	}

	@ParameterizedTest
	@CsvSource({ "Small", "XSmall" })
	void airWithPlasticUsesBubbleWrapBags(Size size) {
		assertThat(selector.choose(size, ShippingMode.Air).protections())
				.containsExactly(Protection.BubbleWrapBags);
	}

	@ParameterizedTest
	@CsvSource({ "XLarge", "Medium", "XSmall" })
	void landUsesPolystyreneBallsForAnyPackage(Size size) {
		assertThat(selector.choose(size, ShippingMode.Land).protections())
				.containsExactly(Protection.PolystyreneBalls);
	}

	@ParameterizedTest
	@CsvSource({ "XLarge", "Medium", "XSmall" })
	void seaUsesMoistureBeadsAndBubbleWrapForAnyPackage(Size size) {
		assertThat(selector.choose(size, ShippingMode.Sea).protections())
				.containsExactly(Protection.MoistureAbsorbingBeads, Protection.BubbleWrapBags);
	}

	@Test
	void chooseCombinesPackageAndProtection() {
		Packaging packaging = selector.choose(Size.Medium, ShippingMode.Air);

		assertThat(packaging.packageType()).isEqualTo(PackageType.Cardboard);
		assertThat(packaging.protections()).containsExactly(Protection.PolystyreneBalls);
	}

	@ParameterizedTest
	@EnumSource(Size.class)
	void everySizeHasExactlyOnePackageStrategy(Size size) {
		long matches = List.of(new WoodPackage(), new CardboardPackage(), new PlasticPackage()).stream()
				.filter(strategy -> strategy.appliesTo(size)).count();

		assertThat(matches).isEqualTo(1);
	}

	@ParameterizedTest
	@EnumSource(ShippingMode.class)
	void everyShippingModeHasExactlyOneProtectionStrategy(ShippingMode mode) {
		long matches = List.of(new LandProtection(), new AirProtection(), new SeaProtection()).stream()
				.filter(strategy -> strategy.appliesTo(mode)).count();

		assertThat(matches).isEqualTo(1);
	}

	/** Every size x shipping mode combination, written out from the requirements (protections joined by '|'). */
	@ParameterizedTest
	@CsvSource({
			"XLarge,Land,Wood,PolystyreneBalls",
			"XLarge,Air,Wood,PolystyreneBalls",
			"XLarge,Sea,Wood,MoistureAbsorbingBeads|BubbleWrapBags",
			"Large,Land,Wood,PolystyreneBalls",
			"Large,Air,Wood,PolystyreneBalls",
			"Large,Sea,Wood,MoistureAbsorbingBeads|BubbleWrapBags",
			"Medium,Land,Cardboard,PolystyreneBalls",
			"Medium,Air,Cardboard,PolystyreneBalls",
			"Medium,Sea,Cardboard,MoistureAbsorbingBeads|BubbleWrapBags",
			"Small,Land,Plastic,PolystyreneBalls",
			"Small,Air,Plastic,BubbleWrapBags",
			"Small,Sea,Plastic,MoistureAbsorbingBeads|BubbleWrapBags",
			"XSmall,Land,Plastic,PolystyreneBalls",
			"XSmall,Air,Plastic,BubbleWrapBags",
			"XSmall,Sea,Plastic,MoistureAbsorbingBeads|BubbleWrapBags" })
	void everySizeAndShippingModeGetsExactlyTheRequiredPackaging(Size size, ShippingMode mode,
			PackageType expectedPackage, String expectedProtections) {
		List<Protection> protections = Arrays.stream(expectedProtections.split("\\|")).map(Protection::valueOf)
				.toList();

		Packaging packaging = selector.choose(size, mode);

		assertThat(packaging.packageType()).isEqualTo(expectedPackage);
		assertThat(packaging.protections()).containsExactlyElementsOf(protections);
	}

	@Test
	void missingStrategyFailsLoudlyInsteadOfGuessing() {
		PackagingSelector empty = new PackagingSelector(List.of(), List.of());

		assertThatThrownBy(() -> empty.choose(Size.Medium, ShippingMode.Air))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Medium");
	}
}
