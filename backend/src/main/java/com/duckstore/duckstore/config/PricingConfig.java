package com.duckstore.duckstore.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.duckstore.duckstore.domain.packaging.PackagingSelector;
import com.duckstore.duckstore.domain.pricing.DestinationRule;
import com.duckstore.duckstore.domain.pricing.PackageRule;
import com.duckstore.duckstore.domain.pricing.PricingCalculator;
import com.duckstore.duckstore.domain.pricing.ShippingRule;
import com.duckstore.duckstore.domain.pricing.VolumeDiscountRule;

/** The only place where the framework-free domain objects are created and wired to the configuration. */
@Configuration
@EnableConfigurationProperties(PricingProperties.class)
class PricingConfig {

	@Bean
	PricingCalculator pricingCalculator(PricingProperties properties) {
		PricingProperties.VolumeDiscount volume = properties.volumeDiscount();
		PricingProperties.Destination destination = properties.destination();
		PricingProperties.Shipping shipping = properties.shipping();
		return PricingCalculator.of(
				new VolumeDiscountRule(volume.threshold(), volume.percent()),
				new PackageRule(properties.packagePercent()),
				new DestinationRule(destination.percent(), destination.defaultPercent()),
				new ShippingRule(shipping.seaFlatFee(), shipping.landPerUnit(), shipping.airPerUnit(),
						shipping.airBulkThreshold(), shipping.airBulkReductionPercent()));
	}

	@Bean
	PackagingSelector packagingSelector() {
		return PackagingSelector.standard();
	}
}
