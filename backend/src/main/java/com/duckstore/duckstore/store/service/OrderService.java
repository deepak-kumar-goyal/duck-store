package com.duckstore.duckstore.store.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duckstore.duckstore.domain.packaging.Packaging;
import com.duckstore.duckstore.domain.packaging.PackagingSelector;
import com.duckstore.duckstore.domain.pricing.OrderDetails;
import com.duckstore.duckstore.domain.pricing.PriceBreakdown;
import com.duckstore.duckstore.domain.pricing.PricingCalculator;
import com.duckstore.duckstore.store.dto.OrderQuoteResponse;
import com.duckstore.duckstore.store.dto.OrderRequest;
import com.duckstore.duckstore.warehouse.model.Duck;
import com.duckstore.duckstore.warehouse.repository.DuckRepository;
import com.duckstore.duckstore.web.ConflictException;
import com.duckstore.duckstore.web.NotFoundException;

@Service
public class OrderService {

	private final DuckRepository repository;
	private final PackagingSelector packagingSelector;
	private final PricingCalculator calculator;

	public OrderService(DuckRepository repository, PackagingSelector packagingSelector,
			PricingCalculator calculator) {
		this.repository = repository;
		this.packagingSelector = packagingSelector;
		this.calculator = calculator;
	}

	/**
	 * Packages and prices the order without touching the stock.
	 * <p>
	 * DECISION: the requirements describe a pricing API, so this endpoint has no side effects and can be called
	 * repeatedly with the same order. Taking stock is a separate, explicit operation: {@link #placeOrder}.
	 */
	@Transactional(readOnly = true)
	public OrderQuoteResponse quote(OrderRequest request) {
		return price(request, resolveDuck(request));
	}

	/**
	 * Same result as a quote, but the ordered units are taken from stock in one atomic step.
	 * <p>
	 * DECISION (beyond the requirements): without this, two quotes could both promise the last units. The price is
	 * read first and the stock is then taken with a conditional update, so concurrent orders can never take the
	 * same units; the loser gets a 409 and can retry. Orders themselves are not stored (no such module is asked for).
	 */
	@Transactional
	public OrderQuoteResponse placeOrder(OrderRequest request) {
		Duck duck = resolveDuck(request);
		if (repository.takeStock(duck.getId(), request.quantity()) == 0) {
			throw new ConflictException("The stock changed while placing the order; please try again");
		}
		return price(request, duck);
	}

	private OrderQuoteResponse price(OrderRequest request, Duck duck) {
		Packaging packaging = packagingSelector.choose(request.size(), request.shippingMode());
		PriceBreakdown price = calculator.calculate(new OrderDetails(request.quantity(), duck.getPrice(),
				request.country(), request.shippingMode(), packaging.packageType()));
		return new OrderQuoteResponse(packaging.packageType(), packaging.protections(), duck.getPrice(),
				price.baseCost(), price.adjustments(), price.total());
	}

	/**
	 * DECISION: the order carries no price and several ducks can share a color and size with different prices. The
	 * cheapest non-deleted duck that can fill the whole order is used (never a mix of prices), ties go to the
	 * lowest id. An order that no single duck record can fill is rejected rather than priced.
	 */
	private Duck resolveDuck(OrderRequest request) {
		return repository.findCheapestInStock(request.color(), request.size(), request.quantity())
				.orElseThrow(() -> unavailable(request));
	}

	/** DECISION: "we do not sell this" (404) is told apart from "we sell it, but not that many" (409). */
	private RuntimeException unavailable(OrderRequest request) {
		String what = request.color() + " " + request.size() + " ducks";
		if (repository.existsByColorAndSizeAndDeletedFalse(request.color(), request.size())) {
			return new ConflictException("Not enough stock of " + what + " for " + request.quantity() + " units");
		}
		return new NotFoundException("No " + what + " available");
	}
}
