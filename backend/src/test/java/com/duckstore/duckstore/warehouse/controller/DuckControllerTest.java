package com.duckstore.duckstore.warehouse.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.duckstore.duckstore.IntegrationTest;
import com.duckstore.duckstore.domain.model.Color;
import com.duckstore.duckstore.domain.model.Size;
import com.duckstore.duckstore.warehouse.model.Duck;

class DuckControllerTest extends IntegrationTest {

	@Autowired
	MockMvc mockMvc;

	private ResultActions add(String json) throws Exception {
		return mockMvc.perform(post("/api/ducks").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private static String duckJson(String color, String size, String price, int quantity) {
		return """
				{"color":"%s","size":"%s","price":%s,"quantity":%d}""".formatted(color, size, price, quantity);
	}

	@Test
	void addReturnsCreatedForANewDuck() throws Exception {
		add(duckJson("Red", "XLarge", "200.00", 10))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.color").value("Red"))
				.andExpect(jsonPath("$.size").value("XLarge"))
				.andExpect(jsonPath("$.price").value(200.00))
				.andExpect(jsonPath("$.quantity").value(10));
	}

	@Test
	void addReturnsOkWhenTheQuantityWasMergedIntoAnExistingDuck() throws Exception {
		add(duckJson("Red", "XLarge", "200.00", 10)).andExpect(status().isCreated());

		add(duckJson("Red", "XLarge", "200.00", 5))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity").value(15));
	}

	@Test
	void addRejectsUnknownColor() throws Exception {
		add(duckJson("Purple", "XLarge", "200.00", 10)).andExpect(status().isBadRequest());
	}

	@Test
	void addRejectsNonPositiveQuantityAndReportsTheField() throws Exception {
		add(duckJson("Red", "XLarge", "200.00", 0))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.quantity").exists());
	}

	@Test
	void addRejectsQuantityAboveTheMaximum() throws Exception {
		add(duckJson("Red", "XLarge", "200.00", Duck.MAX_QUANTITY + 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.quantity").exists());
	}

	@Test
	void addReturnsConflictWhenTheMergedTotalWouldExceedTheMaximum() throws Exception {
		add(duckJson("Red", "XLarge", "200.00", Duck.MAX_QUANTITY)).andExpect(status().isCreated());

		add(duckJson("Red", "XLarge", "200.00", 1))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("The total quantity of a duck cannot exceed 1000000"));
	}

	@Test
	void addRejectsPriceWithMoreThanTwoDecimals() throws Exception {
		add(duckJson("Red", "XLarge", "10.999", 1))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.price").exists());
	}

	@Test
	void addRejectsMissingFields() throws Exception {
		add("{}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.color").exists());
	}

	@Test
	void listReturnsDucksSortedByQuantity() throws Exception {
		add(duckJson("Red", "XLarge", "200.00", 20));
		add(duckJson("Green", "Large", "100.00", 5));

		mockMvc.perform(get("/api/ducks"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].quantity").value(5))
				.andExpect(jsonPath("$[1].quantity").value(20));
	}

	@Test
	void updateOfUnknownDuckReturnsNotFound() throws Exception {
		mockMvc.perform(put("/api/ducks/999").contentType(MediaType.APPLICATION_JSON)
				.content("{\"price\":1.00,\"quantity\":1}"))
				.andExpect(status().isNotFound());
	}

	private ResultActions update(int id, String json) throws Exception {
		return mockMvc.perform(put("/api/ducks/" + id).contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private int savedDuck() {
		return duckRepository.save(new Duck(Color.Red, Size.XLarge, new BigDecimal("200.00"), 10)).getId();
	}

	@ParameterizedTest
	@CsvSource({
			"0,5,price",
			"-1.00,5,price",
			"10.999,5,price",
			"10.00,-1,quantity",
			"10.00,1000001,quantity" })
	void updateRejectsInvalidValuesAndLeavesTheDuckUntouched(String price, String quantity, String badField)
			throws Exception {
		int id = savedDuck();

		update(id, "{\"price\":%s,\"quantity\":%s}".formatted(price, quantity))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors." + badField).exists());

		Duck unchanged = duckRepository.findById(id).orElseThrow();
		assertThat(unchanged.getPrice()).isEqualByComparingTo("200.00");
		assertThat(unchanged.getQuantity()).isEqualTo(10);
	}

	@Test
	void updateRejectsMissingFields() throws Exception {
		update(savedDuck(), "{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.price").exists())
				.andExpect(jsonPath("$.fieldErrors.quantity").exists());
	}

	@Test
	void updateAcceptsZeroQuantityAndTheDuckStaysListed() throws Exception {
		int id = savedDuck();

		update(id, "{\"price\":200.00,\"quantity\":0}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity").value(0));

		mockMvc.perform(get("/api/ducks"))
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].quantity").value(0));
	}

	@Test
	void updateCannotChangeColorOrSizeEvenIfTheyAreSent() throws Exception {
		int id = savedDuck();

		update(id, "{\"color\":\"Green\",\"size\":\"Small\",\"price\":150.00,\"quantity\":7}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.color").value("Red"))
				.andExpect(jsonPath("$.size").value("XLarge"))
				.andExpect(jsonPath("$.price").value(150.00))
				.andExpect(jsonPath("$.quantity").value(7));
	}

	@Test
	void updateToExistingPriceReturnsConflictWithAClearMessage() throws Exception {
		add(duckJson("Red", "XLarge", "200.00", 10));
		Integer otherId = duckRepository.save(new Duck(Color.Red, Size.XLarge, new BigDecimal("150.00"), 1)).getId();

		mockMvc.perform(put("/api/ducks/" + otherId).contentType(MediaType.APPLICATION_JSON)
				.content("{\"price\":200.00,\"quantity\":1}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value(
						"Another duck with the same color, size and price already exists (deleted ducks included)"));
	}

	@Test
	void deleteReturnsNoContentAndHidesTheDuck() throws Exception {
		add(duckJson("Red", "XLarge", "200.00", 10));
		Integer id = duckRepository.findAll().get(0).getId();

		mockMvc.perform(delete("/api/ducks/" + id)).andExpect(status().isNoContent());

		mockMvc.perform(get("/api/ducks")).andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void nonNumericIdReturnsBadRequest() throws Exception {
		mockMvc.perform(delete("/api/ducks/abc")).andExpect(status().isBadRequest());
	}
}
