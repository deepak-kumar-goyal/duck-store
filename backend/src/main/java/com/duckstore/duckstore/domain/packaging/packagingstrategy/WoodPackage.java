package com.duckstore.duckstore.domain.packaging.packagingstrategy;

import java.util.EnumSet;
import java.util.Set;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Size;

/** XLarge and Large ducks go in a wood package. */
public class WoodPackage implements PackagingStrategy {

	private static final Set<Size> SIZES = EnumSet.of(Size.XLarge, Size.Large);

	@Override
	public boolean appliesTo(Size size) {
		return SIZES.contains(size);
	}

	@Override
	public PackageType packageType() {
		return PackageType.Wood;
	}
}
