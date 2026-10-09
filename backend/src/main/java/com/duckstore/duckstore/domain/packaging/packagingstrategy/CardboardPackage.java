package com.duckstore.duckstore.domain.packaging.packagingstrategy;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Size;

/** Medium ducks go in a cardboard package. */
public class CardboardPackage implements PackagingStrategy {

	@Override
	public boolean appliesTo(Size size) {
		return size == Size.Medium;
	}

	@Override
	public PackageType packageType() {
		return PackageType.Cardboard;
	}
}
