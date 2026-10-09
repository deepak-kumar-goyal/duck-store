package com.duckstore.duckstore.store.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.duckstore.duckstore.IntegrationTest;
import com.duckstore.duckstore.domain.model.Color;
import com.duckstore.duckstore.domain.model.Size;
import com.duckstore.duckstore.warehouse.model.Duck;

class OrderControllerTest extends IntegrationTest {

	@Autowired
	MockMvc mockMvc;

	private void stock(Color color, Size size, String price, int quantity) {
		duckRepository.save(new Duck(color, size, new BigDecimal(price), quantity));
	}

	private ResultActions quote(String json) throws Exception {
		return mockMvc.perform(post("/api/orders/quote").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions placeOrder(String json) throws Exception {
		return mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private int stockOf(Color color, Size size) {
		return duckRepository.findAll().stream()
				.filter(duck -> duck.getColor() == color && duck.getSize() == size)
				.mapToInt(Duck::getQuantity).sum();
	}

	private static String order(String color, String size, int quantity, String country, String mode) {
		return """
				{"color":"%s","size":"%s","quantity":%d,"country":"%s","shippingMode":"%s"}"""
				.formatted(color, size, quantity, country, mode);
	}

	@Test
	void quoteReturnsPackageProtectionTotalAndBreakdown() throws Exception {
		stock(Color.Red, Size.XLarge, "200.00", 10000);

		quote(order("Red", "XLarge", 10, "India", "Land"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.packageType").value("Wood"))
				.andExpect(jsonPath("$.protections[0]").value("PolystyreneBalls"))
				.andExpect(jsonPath("$.unitPrice").value(200.00))
				.andExpect(jsonPath("$.baseCost").value(2000.00))
				.andExpect(jsonPath("$.breakdown.length()").value(3))
				.andExpect(jsonPath("$.breakdown[0].amount").value(100.00))
				.andExpect(jsonPath("$.total").value(2580.00));
	}

	@Test
	void quoteTakesPackageAndDestinationPercentagesFromTheDiscountedSubtotalForAWoodOrder() throws Exception {
		stock(Color.Red, Size.XLarge, "10.00", 1000);

		// base 2000, volume -400 -> subtotal 1600; wood +80; USA +288; land +2000
		quote(order("Red", "XLarge", 200, "USA", "Land"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.baseCost").value(2000.00))
				.andExpect(jsonPath("$.breakdown.length()").value(4))
				.andExpect(jsonPath("$.breakdown[0].amount").value(-400.00))
				.andExpect(jsonPath("$.breakdown[1].amount").value(80.00))
				.andExpect(jsonPath("$.breakdown[2].amount").value(288.00))
				.andExpect(jsonPath("$.breakdown[3].amount").value(2000.00))
				.andExpect(jsonPath("$.total").value(3968.00));
	}

	@Test
	void quoteTakesPackageAndDestinationPercentagesFromTheDiscountedSubtotalForAPlasticOrder() throws Exception {
		stock(Color.Green, Size.Small, "4.00", 1000);

		// base 600, volume -120 -> subtotal 480; plastic +48; Bolivia +62.40; air 150 x 30
		quote(order("Green", "Small", 150, "Bolivia", "Air"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.packageType").value("Plastic"))
				.andExpect(jsonPath("$.protections[0]").value("BubbleWrapBags"))
				.andExpect(jsonPath("$.breakdown[0].amount").value(-120.00))
				.andExpect(jsonPath("$.breakdown[1].amount").value(48.00))
				.andExpect(jsonPath("$.breakdown[2].amount").value(62.40))
				.andExpect(jsonPath("$.breakdown[3].amount").value(4500.00))
				.andExpect(jsonPath("$.total").value(5090.40));
	}

	@Test
	void quoteUsesTheCheapestPriceThatCanFillTheWholeOrder() throws Exception {
		stock(Color.Green, Size.Small, "5.00", 5);
		stock(Color.Green, Size.Small, "8.00", 50);
		stock(Color.Green, Size.Small, "9.00", 500);

		quote(order("Green", "Small", 10, "USA", "Sea"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unitPrice").value(8.00));
	}

	@Test
	void quoteIgnoresLogicallyDeletedDucks() throws Exception {
		Duck cheap = new Duck(Color.Black, Size.Medium, new BigDecimal("1.00"), 100);
		cheap.markDeleted();
		duckRepository.save(cheap);
		stock(Color.Black, Size.Medium, "4.00", 100);

		quote(order("Black", "Medium", 1, "India", "Air"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unitPrice").value(4.00));
	}

	@Test
	void quoteForColorAndSizeNotInTheWarehouseIsNotFound() throws Exception {
		quote(order("Red", "XLarge", 1, "India", "Land")).andExpect(status().isNotFound());
	}

	@Test
	void quoteWithInsufficientStockIsAConflict() throws Exception {
		stock(Color.Red, Size.XLarge, "200.00", 5);

		quote(order("Red", "XLarge", 6, "India", "Land")).andExpect(status().isConflict());
	}

	@Test
	void aDuckWithZeroStockIsOutOfStockNotMissing() throws Exception {
		stock(Color.Red, Size.XLarge, "200.00", 0);

		quote(order("Red", "XLarge", 1, "India", "Land"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(containsString("Not enough stock")));
		placeOrder(order("Red", "XLarge", 1, "India", "Land")).andExpect(status().isConflict());

		assertThat(stockOf(Color.Red, Size.XLarge)).isZero();
	}

	@Test
	void anOrderThatEmptiesTheStockMakesTheNextOrderOutOfStockNotMissing() throws Exception {
		stock(Color.Green, Size.Small, "5.00", 3);
		placeOrder(order("Green", "Small", 3, "USA", "Sea")).andExpect(status().isCreated());

		placeOrder(order("Green", "Small", 1, "USA", "Sea")).andExpect(status().isConflict());
	}

	@Test
	void quoteRejectsNonPositiveQuantity() throws Exception {
		quote(order("Red", "XLarge", 0, "India", "Land"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.quantity").exists());
	}

	@Test
	void quoteRejectsBlankCountry() throws Exception {
		quote(order("Red", "XLarge", 1, " ", "Land"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.country").exists());
	}

	@Test
	void quoteRejectsUnknownShippingMode() throws Exception {
		quote(order("Red", "XLarge", 1, "India", "Rocket")).andExpect(status().isBadRequest());
	}

	@Test
	void quoteDoesNotChangeTheStock() throws Exception {
		stock(Color.Red, Size.XLarge, "200.00", 10);

		quote(order("Red", "XLarge", 4, "India", "Land")).andExpect(status().isOk());

		assertThat(stockOf(Color.Red, Size.XLarge)).isEqualTo(10);
	}

	@Test
	void placeOrderReturnsTheSamePricingAsAQuoteAndTakesTheStock() throws Exception {
		stock(Color.Red, Size.XLarge, "200.00", 100);

		placeOrder(order("Red", "XLarge", 10, "India", "Land"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.packageType").value("Wood"))
				.andExpect(jsonPath("$.total").value(2580.00));

		assertThat(stockOf(Color.Red, Size.XLarge)).isEqualTo(90);
	}

	@Test
	void placeOrderCannotTakeMoreThanIsLeft() throws Exception {
		stock(Color.Red, Size.XLarge, "200.00", 10);
		placeOrder(order("Red", "XLarge", 7, "India", "Land")).andExpect(status().isCreated());

		placeOrder(order("Red", "XLarge", 4, "India", "Land")).andExpect(status().isConflict());

		assertThat(stockOf(Color.Red, Size.XLarge)).isEqualTo(3);
	}

	@Test
	void placeOrderCanTakeTheLastUnits() throws Exception {
		stock(Color.Green, Size.Small, "5.00", 3);

		placeOrder(order("Green", "Small", 3, "USA", "Sea")).andExpect(status().isCreated());

		assertThat(stockOf(Color.Green, Size.Small)).isZero();
	}

	@Test
	void placeOrderRejectsInvalidRequestsWithoutTouchingTheStock() throws Exception {
		stock(Color.Red, Size.XLarge, "200.00", 10);

		placeOrder(order("Red", "XLarge", 0, "India", "Land")).andExpect(status().isBadRequest());

		assertThat(stockOf(Color.Red, Size.XLarge)).isEqualTo(10);
	}
}
