package com.duckstore.duckstore.domain.packaging.protectionstrategy;

import java.util.List;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Protection;
import com.duckstore.duckstore.domain.model.ShippingMode;

/** Sea shipment, any package: moisture-absorbing beads and bubble-wrap bags. */
public class SeaProtection implements ProtectionStrategy {

	@Override
	public boolean appliesTo(ShippingMode mode) {
		return mode == ShippingMode.Sea;
	}

	@Override
	public List<Protection> protectionFor(PackageType packageType) {
		return List.of(Protection.MoistureAbsorbingBeads, Protection.BubbleWrapBags);
	}
}
