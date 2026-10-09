package com.duckstore.duckstore.domain.packaging.protectionstrategy;

import java.util.List;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Protection;
import com.duckstore.duckstore.domain.model.ShippingMode;

/** Air shipment: polystyrene balls for wood or cardboard, bubble-wrap bags for plastic. */
public class AirProtection implements ProtectionStrategy {

	@Override
	public boolean appliesTo(ShippingMode mode) {
		return mode == ShippingMode.Air;
	}

	@Override
	public List<Protection> protectionFor(PackageType packageType) {
		return packageType == PackageType.Plastic
				? List.of(Protection.BubbleWrapBags)
				: List.of(Protection.PolystyreneBalls);
	}
}
