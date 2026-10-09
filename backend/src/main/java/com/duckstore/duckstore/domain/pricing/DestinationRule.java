package com.duckstore.duckstore.domain.pricing;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adds a percentage by destination country, or {@code defaultPercent} for any other country. The requirements set
 * USA +18%, Bolivia +13%, India +19% and +15% otherwise; the values come from {@code pricing.destination.*} in
 * application.properties. Country names are case-insensitive.
 * <p>
 * DECISION: the requirements do not define how countries are written. Only the configured names (by default "USA",
 * "Bolivia" and "India", ignoring case and surrounding spaces) are special; aliases such as "United States" count as
 * "other". An unknown or misspelled country is never an error, it simply gets the default percentage.
 * <p>
 * FUTURE SCOPE: countries are still matched by free-text name. They could be keyed by country code (for example
 * ISO 3166) or read from a database table, which would remove the need to match free text and let aliases such as
 * "United States" resolve to the same entry as "USA".
 */
public class DestinationRule implements PricingRule {

	private final Map<String, Integer> percentByCountry;
	private final int defaultPercent;

	public DestinationRule(Map<String, Integer> percentByCountry, int defaultPercent) {
		Map<String, Integer> normalized = new HashMap<>();
		percentByCountry.forEach((country, percent) -> normalized.put(normalize(country), percent));
		this.percentByCountry = Map.copyOf(normalized);
		this.defaultPercent = defaultPercent;
	}

	@Override
	public List<Adjustment> apply(OrderDetails order, BigDecimal subtotal) {
		String country = order.country().trim();
		int percent = percentByCountry.getOrDefault(normalize(country), defaultPercent);
		return List.of(Adjustment.percentage("Destination " + country, subtotal, percent));
	}

	private static String normalize(String country) {
		return country.trim().toUpperCase(Locale.ROOT);
	}
}
