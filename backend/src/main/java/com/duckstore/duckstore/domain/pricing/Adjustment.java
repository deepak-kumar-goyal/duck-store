package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;

/** One line of the price breakdown. A negative amount is a discount, a positive amount is an increment. */
public record Adjustment(String description, BigDecimal amount) {

	public static Adjustment fixed(String description, BigDecimal amount) {
		return new Adjustment(description, Money.round(amount));
	}

	/** A signed percentage of {@code base}, e.g. {@code -20} is a 20% discount and {@code 5} a 5% increment. */
	public static Adjustment percentage(String description, BigDecimal base, int percent) {
		String sign = percent > 0 ? "+" : "";
		return new Adjustment(description + " (" + sign + percent + "%)", Money.percentOf(base, percent));
	}
}
