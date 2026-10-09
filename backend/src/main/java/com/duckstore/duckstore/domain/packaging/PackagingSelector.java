package com.duckstore.duckstore.domain.packaging;

import java.util.List;
import java.util.function.Predicate;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.ShippingMode;
import com.duckstore.duckstore.domain.model.Size;
import com.duckstore.duckstore.domain.packaging.packagingstrategy.CardboardPackage;
import com.duckstore.duckstore.domain.packaging.packagingstrategy.PackagingStrategy;
import com.duckstore.duckstore.domain.packaging.packagingstrategy.PlasticPackage;
import com.duckstore.duckstore.domain.packaging.packagingstrategy.WoodPackage;
import com.duckstore.duckstore.domain.packaging.protectionstrategy.AirProtection;
import com.duckstore.duckstore.domain.packaging.protectionstrategy.LandProtection;
import com.duckstore.duckstore.domain.packaging.protectionstrategy.ProtectionStrategy;
import com.duckstore.duckstore.domain.packaging.protectionstrategy.SeaProtection;

/**
 * Chooses the package from the size, then the protection from the shipping mode and that package. Each decision is
 * made by the first matching strategy.
 */
public class PackagingSelector {

	private final List<PackagingStrategy> packagingStrategies;
	private final List<ProtectionStrategy> protectionStrategies;

	PackagingSelector(List<PackagingStrategy> packagingStrategies, List<ProtectionStrategy> protectionStrategies) {
		this.packagingStrategies = packagingStrategies;
		this.protectionStrategies = protectionStrategies;
	}

	public static PackagingSelector standard() {
		return new PackagingSelector(
				List.of(new WoodPackage(), new CardboardPackage(), new PlasticPackage()),
				List.of(new LandProtection(), new AirProtection(), new SeaProtection()));
	}

	public Packaging choose(Size size, ShippingMode mode) {
		PackageType packageType = firstMatch(packagingStrategies, s -> s.appliesTo(size), size).packageType();
		return new Packaging(packageType, firstMatch(protectionStrategies, s -> s.appliesTo(mode), mode)
				.protectionFor(packageType));
	}

	private static <T> T firstMatch(List<T> strategies, Predicate<T> matches, Object input) {
		return strategies.stream().filter(matches).findFirst()
				.orElseThrow(() -> new IllegalStateException("No packaging strategy for " + input));
	}
}
