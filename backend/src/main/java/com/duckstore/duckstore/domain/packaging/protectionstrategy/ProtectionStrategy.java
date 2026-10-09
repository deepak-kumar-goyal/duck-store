package com.duckstore.duckstore.domain.packaging.protectionstrategy;

import java.util.List;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Protection;
import com.duckstore.duckstore.domain.model.ShippingMode;

/** Decides what fills the package for the shipping mode it handles. */
public interface ProtectionStrategy {

	boolean appliesTo(ShippingMode mode);

	List<Protection> protectionFor(PackageType packageType);
}
