package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.ShippingMode;

public record OrderDetails(
		int quantity,
		BigDecimal unitPrice,
		String country,
		ShippingMode shippingMode,
		PackageType packageType) {

	public BigDecimal baseCost() {
		return Money.round(unitPrice.multiply(BigDecimal.valueOf(quantity)));
	}
}
