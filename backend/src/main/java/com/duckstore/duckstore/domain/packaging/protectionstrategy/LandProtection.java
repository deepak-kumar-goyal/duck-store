package com.duckstore.duckstore.domain.packaging.protectionstrategy;

import java.util.List;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Protection;
import com.duckstore.duckstore.domain.model.ShippingMode;

/** Land shipment, any package: polystyrene balls. */
public class LandProtection implements ProtectionStrategy {

	@Override
	public boolean appliesTo(ShippingMode mode) {
		return mode == ShippingMode.Land;
	}

	@Override
	public List<Protection> protectionFor(PackageType packageType) {
		return List.of(Protection.PolystyreneBalls);
	}
}
