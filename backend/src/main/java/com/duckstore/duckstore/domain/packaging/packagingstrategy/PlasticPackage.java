package com.duckstore.duckstore.domain.packaging.packagingstrategy;

import java.util.EnumSet;
import java.util.Set;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Size;

/** Small and XSmall ducks go in a plastic package. */
public class PlasticPackage implements PackagingStrategy {

	private static final Set<Size> SIZES = EnumSet.of(Size.Small, Size.XSmall);

	@Override
	public boolean appliesTo(Size size) {
		return SIZES.contains(size);
	}

	@Override
	public PackageType packageType() {
		return PackageType.Plastic;
	}
}
