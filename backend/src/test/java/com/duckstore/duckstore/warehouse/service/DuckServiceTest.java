package com.duckstore.duckstore.warehouse.service;

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
import com.duckstore.duckstore.domain.model.Size;
import com.duckstore.duckstore.warehouse.dto.CreateDuckRequest;
import com.duckstore.duckstore.warehouse.dto.DuckResponse;
import com.duckstore.duckstore.warehouse.dto.UpdateDuckRequest;
import com.duckstore.duckstore.warehouse.model.Duck;
import com.duckstore.duckstore.web.ConflictException;
import com.duckstore.duckstore.web.NotFoundException;

class DuckServiceTest extends IntegrationTest {

	@Autowired
	DuckService service;

	private static CreateDuckRequest request(Color color, Size size, String price, int quantity) {
		return new CreateDuckRequest(color, size, new BigDecimal(price), quantity);
	}

	private AddDuckResult addResult(Color color, Size size, String price, int quantity) {
		return service.add(request(color, size, price, quantity));
	}

	private DuckResponse add(Color color, Size size, String price, int quantity) {
		return addResult(color, size, price, quantity).duck();
	}

	@Test
	void addCreatesNewDuckWhenNoneMatches() {
		AddDuckResult result = addResult(Color.Red, Size.XLarge, "200.00", 10);

		assertThat(result.created()).isTrue();
		assertThat(result.duck().id()).isNotNull();
		assertThat(result.duck().quantity()).isEqualTo(10);
		assertThat(duckRepository.count()).isEqualTo(1);
	}

	@Test
	void addMergesQuantityWhenColorSizeAndPriceMatch() {
		DuckResponse first = add(Color.Red, Size.XLarge, "200.00", 10);
		AddDuckResult second = addResult(Color.Red, Size.XLarge, "200", 5);

		assertThat(second.created()).isFalse();
		assertThat(second.duck().id()).isEqualTo(first.id());
		assertThat(second.duck().quantity()).isEqualTo(15);
		assertThat(duckRepository.count()).isEqualTo(1);
	}

	@Test
	void addCreatesSeparateRecordWhenPriceDiffers() {
		add(Color.Red, Size.XLarge, "200.00", 10);
		AddDuckResult second = addResult(Color.Red, Size.XLarge, "199.99", 10);

		assertThat(second.created()).isTrue();
		assertThat(duckRepository.count()).isEqualTo(2);
	}

	@Test
	void addCreatesSeparateRecordWhenColorOrSizeDiffers() {
		add(Color.Red, Size.XLarge, "200.00", 10);
		add(Color.Green, Size.XLarge, "200.00", 10);
		add(Color.Red, Size.Large, "200.00", 10);

		assertThat(duckRepository.count()).isEqualTo(3);
	}

	@Test
	void concurrentAddsOfSameDuckNeverDuplicateAndNeverLoseQuantity() throws Exception {
		int threads = 20;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<?>> results = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			results.add(executor.submit(() -> {
				start.await();
				return addResult(Color.Yellow, Size.Medium, "12.50", 5);
			}));
		}
		start.countDown();
		for (Future<?> result : results) {
			result.get();
		}
		executor.shutdown();

		List<Duck> ducks = duckRepository.findAll();
		assertThat(ducks).hasSize(1);
		assertThat(ducks.get(0).getQuantity()).isEqualTo(threads * 5);
	}

	@Test
	void exactlyOneOfManyConcurrentFirstAddsReportsCreated() throws Exception {
		int threads = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<AddDuckResult>> results = new ArrayList<>();
		for (int i = 0; i < threads; i++) {
			results.add(executor.submit(() -> {
				start.await();
				return addResult(Color.Black, Size.Large, "9.99", 1);
			}));
		}
		start.countDown();
		long created = 0;
		for (Future<AddDuckResult> result : results) {
			created += result.get().created() ? 1 : 0;
		}
		executor.shutdown();

		assertThat(created).isEqualTo(1);
	}

	@Test
	void addRevivesLogicallyDeletedDuckWithOnlyTheNewQuantity() {
		DuckResponse original = add(Color.Black, Size.Small, "5.00", 40);
		service.delete(original.id());

		DuckResponse revived = add(Color.Black, Size.Small, "5.00", 3);

		assertThat(revived.id()).isEqualTo(original.id());
		assertThat(revived.quantity()).isEqualTo(3);
		assertThat(service.list()).hasSize(1);
	}

	@Test
	void addRejectsATotalQuantityAboveTheMaximum() {
		add(Color.Red, Size.XLarge, "200.00", Duck.MAX_QUANTITY - 1);

		assertThatThrownBy(() -> add(Color.Red, Size.XLarge, "200.00", 2))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining(String.valueOf(Duck.MAX_QUANTITY));
		assertThat(duckRepository.findAll().get(0).getQuantity()).isEqualTo(Duck.MAX_QUANTITY - 1);
	}

	@Test
	void addAcceptsATotalQuantityExactlyAtTheMaximum() {
		add(Color.Red, Size.XLarge, "200.00", Duck.MAX_QUANTITY - 1);

		assertThat(add(Color.Red, Size.XLarge, "200.00", 1).quantity()).isEqualTo(Duck.MAX_QUANTITY);
	}

	@Test
	void listExcludesDeletedAndSortsByQuantityAscending() {
		add(Color.Red, Size.XLarge, "1.00", 30);
		DuckResponse deleted = add(Color.Green, Size.XLarge, "1.00", 5);
		add(Color.Black, Size.XLarge, "1.00", 10);
		service.delete(deleted.id());

		List<DuckResponse> ducks = service.list();

		assertThat(ducks).extracting(DuckResponse::quantity).containsExactly(10, 30);
	}

	@Test
	void updateChangesPriceAndQuantityButNotColorOrSize() {
		DuckResponse duck = add(Color.Red, Size.XLarge, "200.00", 10);

		DuckResponse updated = service.update(duck.id(), new UpdateDuckRequest(new BigDecimal("150.50"), 7));

		assertThat(updated.price()).isEqualByComparingTo("150.50");
		assertThat(updated.quantity()).isEqualTo(7);
		assertThat(updated.color()).isEqualTo(Color.Red);
		assertThat(updated.size()).isEqualTo(Size.XLarge);
	}

	@Test
	void updateKeepingTheSamePriceIsNotAConflictWithItself() {
		DuckResponse duck = add(Color.Red, Size.XLarge, "200.00", 10);

		DuckResponse updated = service.update(duck.id(), new UpdateDuckRequest(new BigDecimal("200.00"), 3));

		assertThat(updated.quantity()).isEqualTo(3);
	}

	@Test
	void updateToPriceOfAnotherDuckWithSameColorAndSizeIsRejectedWithAClearMessage() {
		add(Color.Red, Size.XLarge, "200.00", 10);
		DuckResponse other = add(Color.Red, Size.XLarge, "150.00", 10);

		assertThatThrownBy(() -> service.update(other.id(), new UpdateDuckRequest(new BigDecimal("200.00"), 10)))
				.isInstanceOf(ConflictException.class)
				.hasMessageContaining("same color, size and price");
	}

	@Test
	void updateToPriceOfALogicallyDeletedDuckIsRejected() {
		DuckResponse deleted = add(Color.Red, Size.XLarge, "200.00", 10);
		service.delete(deleted.id());
		DuckResponse other = add(Color.Red, Size.XLarge, "150.00", 10);

		assertThatThrownBy(() -> service.update(other.id(), new UpdateDuckRequest(new BigDecimal("200.00"), 10)))
				.isInstanceOf(ConflictException.class);
	}

	@Test
	void updateUnknownDuckFails() {
		assertThatThrownBy(() -> service.update(999, new UpdateDuckRequest(BigDecimal.ONE, 1)))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	void staleEditAfterAnotherEditIsRejected() {
		DuckResponse duck = add(Color.Red, Size.XLarge, "200.00", 10);
		Duck stale = duckRepository.findById(duck.id()).orElseThrow();
		service.update(duck.id(), new UpdateDuckRequest(new BigDecimal("200.00"), 20));

		stale.update(new BigDecimal("200.00"), 99);

		assertThatThrownBy(() -> duckRepository.saveAndFlush(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	@Test
	void staleEditAfterAMergingAddIsRejected() {
		DuckResponse duck = add(Color.Red, Size.XLarge, "200.00", 10);
		Duck stale = duckRepository.findById(duck.id()).orElseThrow();
		add(Color.Red, Size.XLarge, "200.00", 5);

		stale.update(new BigDecimal("200.00"), 99);

		assertThatThrownBy(() -> duckRepository.saveAndFlush(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
		assertThat(duckRepository.findById(duck.id()).orElseThrow().getQuantity()).isEqualTo(15);
	}

	@Test
	void deleteIsLogicalAndKeepsTheRecord() {
		DuckResponse duck = add(Color.Red, Size.XLarge, "200.00", 10);

		service.delete(duck.id());

		assertThat(duckRepository.findById(duck.id())).get().extracting(Duck::isDeleted).isEqualTo(true);
		assertThat(service.list()).isEmpty();
	}

	@Test
	void deletingAnAlreadyDeletedDuckFails() {
		DuckResponse duck = add(Color.Red, Size.XLarge, "200.00", 10);
		service.delete(duck.id());

		assertThatThrownBy(() -> service.delete(duck.id())).isInstanceOf(NotFoundException.class);
	}

	@Test
	void updateOfDeletedDuckFails() {
		DuckResponse duck = add(Color.Red, Size.XLarge, "200.00", 10);
		service.delete(duck.id());

		assertThatThrownBy(() -> service.update(duck.id(), new UpdateDuckRequest(BigDecimal.ONE, 1)))
				.isInstanceOf(NotFoundException.class);
	}
}
