package com.duckstore.duckstore.domain.packaging.packagingstrategy;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Size;

/** Decides the package type for the sizes it handles. */
public interface PackagingStrategy {

	boolean appliesTo(Size size);

	PackageType packageType();
}
