package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** All money is a BigDecimal with scale 2, rounded HALF_UP. */
public final class Money {

	/** Decimal places of every amount; also the number of decimals a duck price may have. */
	public static final int SCALE = 2;
	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private Money() {
	}

	public static BigDecimal round(BigDecimal amount) {
		return amount.setScale(SCALE, RoundingMode.HALF_UP);
	}

	public static BigDecimal percentOf(BigDecimal amount, int percent) {
		return round(amount.multiply(BigDecimal.valueOf(percent)).divide(HUNDRED, SCALE, RoundingMode.HALF_UP));
	}
}
