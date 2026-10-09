package com.duckstore.duckstore.warehouse.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.duckstore.duckstore.warehouse.dto.CreateDuckRequest;
import com.duckstore.duckstore.warehouse.dto.DuckResponse;
import com.duckstore.duckstore.warehouse.dto.UpdateDuckRequest;
import com.duckstore.duckstore.warehouse.model.Duck;
import com.duckstore.duckstore.warehouse.repository.DuckRepository;
import com.duckstore.duckstore.web.ConflictException;
import com.duckstore.duckstore.web.NotFoundException;

@Service
public class DuckService {

	private static final int ROW_INSERTED = 1;

	private final DuckRepository repository;

	public DuckService(DuckRepository repository) {
		this.repository = repository;
	}

	/** Adds a new duck, or adds the quantity to the existing duck with the same color, size and price. */
	@Transactional
	public AddDuckResult add(CreateDuckRequest request) {
		int affectedRows = upsert(request);
		Duck duck = repository.findByColorAndSizeAndPrice(request.color(), request.size(), request.price())
				.orElseThrow();
		return new AddDuckResult(DuckResponse.from(duck), affectedRows == ROW_INSERTED);
	}

	@Transactional(readOnly = true)
	public List<DuckResponse> list() {
		return repository.findByDeletedFalseOrderByQuantityAscIdAsc().stream().map(DuckResponse::from).toList();
	}

	@Transactional
	public DuckResponse update(int id, UpdateDuckRequest request) {
		Duck duck = findActive(id);
		rejectPriceUsedByAnotherDuck(duck, request);
		duck.update(request.price(), request.quantity());
		// Flush now so a concurrent change surfaces here instead of after the method returns.
		repository.saveAndFlush(duck);
		return DuckResponse.from(duck);
	}

	/** DECISION: deleting an already deleted duck is a 404, because deleted ducks are invisible to the API. */
	@Transactional
	public void delete(int id) {
		findActive(id).markDeleted();
	}

	/** The only integrity failure an add can hit is the maximum quantity: the unique key is handled by the upsert. */
	private int upsert(CreateDuckRequest request) {
		try {
			return repository.upsert(request.color(), request.size(), request.price(), request.quantity());
		} catch (DataIntegrityViolationException e) {
			throw new ConflictException("The total quantity of a duck cannot exceed " + Duck.MAX_QUANTITY);
		}
	}

	/**
	 * DECISION: the requirements say nothing about an edit that makes a duck identical (color, size, price) to
	 * another one. Silently merging would delete a record behind the user's back, so it is rejected with 409.
	 * Logically deleted ducks count too, because they still occupy the unique key.
	 */
	private void rejectPriceUsedByAnotherDuck(Duck duck, UpdateDuckRequest request) {
		boolean taken = repository.existsByColorAndSizeAndPriceAndIdNot(duck.getColor(), duck.getSize(),
				request.price(), duck.getId());
		if (taken) {
			throw new ConflictException("Another duck with the same color, size and price already exists "
					+ "(deleted ducks included)");
		}
	}

	private Duck findActive(int id) {
		return repository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> new NotFoundException("Duck " + id + " not found"));
	}
}
