package com.duckstore.duckstore.warehouse.dto;

import java.math.BigDecimal;

import com.duckstore.duckstore.domain.model.Color;
import com.duckstore.duckstore.domain.model.Size;
import com.duckstore.duckstore.warehouse.model.Duck;

public record DuckResponse(Integer id, Color color, Size size, BigDecimal price, int quantity) {

	public static DuckResponse from(Duck duck) {
		return new DuckResponse(duck.getId(), duck.getColor(), duck.getSize(), duck.getPrice(), duck.getQuantity());
	}
}
