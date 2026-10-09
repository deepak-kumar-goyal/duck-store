package com.duckstore.duckstore.store.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duckstore.duckstore.store.dto.OrderQuoteResponse;
import com.duckstore.duckstore.store.dto.OrderRequest;
import com.duckstore.duckstore.store.service.OrderService;
import com.duckstore.duckstore.web.ApiPaths;

import jakarta.validation.Valid;

@RestController
@RequestMapping(ApiPaths.ORDERS)
public class OrderController {

	private final OrderService service;

	public OrderController(OrderService service) {
		this.service = service;
	}

	/** Prices the order only; the stock is not changed. */
	@PostMapping(ApiPaths.QUOTE)
	OrderQuoteResponse quote(@Valid @RequestBody OrderRequest request) {
		return service.quote(request);
	}

	/** Prices the order and takes the ordered units from stock. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	OrderQuoteResponse placeOrder(@Valid @RequestBody OrderRequest request) {
		return service.placeOrder(request);
	}
}
