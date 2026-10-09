package com.duckstore.duckstore.store.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.duckstore.duckstore.IntegrationTest;
import com.duckstore.duckstore.domain.model.Color;
import com.duckstore.duckstore.domain.model.ShippingMode;
import com.duckstore.duckstore.domain.model.Size;
import com.duckstore.duckstore.store.dto.OrderRequest;
import com.duckstore.duckstore.warehouse.model.Duck;
import com.duckstore.duckstore.warehouse.service.DuckService;
import com.duckstore.duckstore.web.ConflictException;
import com.duckstore.duckstore.web.NotFoundException;

class OrderServiceTest extends IntegrationTest {

	@Autowired
	OrderService service;

	@Autowired
	DuckService duckService;

	/** An order is either accepted, or rejected because the stock ran out or the duck was deleted meanwhile. */
	private boolean tryPlaceOrder(OrderRequest request) {
		try {
			service.placeOrder(request);
			return true;
		} catch (ConflictException | NotFoundException e) {
			return false;
		}
	}

	@Test
	void anEditBasedOnStaleDataAfterAnOrderIsRejectedInsteadOfRestoringSoldStock() {
		Integer id = duckRepository.save(new Duck(Color.Red, Size.XLarge, new BigDecimal("200.00"), 10)).getId();
		Duck stale = duckRepository.findById(id).orElseThrow();
		service.placeOrder(new OrderRequest(Color.Red, Size.XLarge, 4, "India", ShippingMode.Land));

		stale.update(new BigDecimal("200.00"), 10);

		assertThatThrownBy(() -> duckRepository.saveAndFlush(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
		assertThat(duckRepository.findById(id).orElseThrow().getQuantity()).isEqualTo(6);
	}

	@Test
	void ordersRacingWithADeleteNeverSellDeletedStockOrOverSell() throws Exception {
		int stock = 300;
		int orderThreads = 8;
		int attemptsPerThread = 50;
		Integer id = duckRepository.save(new Duck(Color.Red, Size.XLarge, new BigDecimal("200.00"), stock)).getId();
		OrderRequest oneUnit = new OrderRequest(Color.Red, Size.XLarge, 1, "India", ShippingMode.Land);
		ExecutorService executor = Executors.newFixedThreadPool(orderThreads + 1);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> orders = new ArrayList<>();
		for (int i = 0; i < orderThreads; i++) {
			orders.add(executor.submit(() -> {
				start.await();
				int accepted = 0;
				for (int attempt = 0; attempt < attemptsPerThread; attempt++) {
					accepted += tryPlaceOrder(oneUnit) ? 1 : 0;
				}
				return accepted;
			}));
		}
		Future<?> delete = executor.submit(() -> {
			start.await();
			deleteRetryingOnStaleData(id);
			return null;
		});
		start.countDown();
		int accepted = 0;
		for (Future<Integer> result : orders) {
			accepted += result.get();
		}
		delete.get();
		executor.shutdown();

		Duck duck = duckRepository.findById(id).orElseThrow();
		assertThat(duck.isDeleted()).isTrue();
		// every accepted order took exactly one unit; nothing was sold twice or after the delete
		assertThat(accepted + duck.getQuantity()).isEqualTo(stock);
		assertThat(duck.getQuantity()).isNotNegative();
	}

	/** A delete based on data that an order just changed fails with an optimistic lock error; a client would retry. */
	private void deleteRetryingOnStaleData(Integer id) {
		for (int attempt = 0; attempt < 1000; attempt++) {
			try {
				duckService.delete(id);
				return;
			} catch (ObjectOptimisticLockingFailureException e) {
				// reload and try again
			}
		}
		throw new IllegalStateException("Delete never succeeded");
	}

	@Test
	void concurrentOrdersNeverTakeMoreThanTheStock() throws Exception {
		int stock = 10;
		int threads = 25;
		duckRepository.save(new Duck(Color.Red, Size.XLarge, new BigDecimal("200.00"), stock));
		OrderRequest oneUnit = new OrderRequest(Color.Red, Size.XLarge, 1, "India", ShippingMode.Land);
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			results.add(executor.submit(() -> {
				start.await();
				return tryPlaceOrder(oneUnit);
			}));
		}
		start.countDown();
		long accepted = 0;
		for (Future<Boolean> result : results) {
			accepted += result.get() ? 1 : 0;
		}
		executor.shutdown();

		assertThat(accepted).isEqualTo(stock);
		assertThat(duckRepository.findAll().get(0).getQuantity()).isZero();
	}
}
