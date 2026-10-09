package com.duckstore.duckstore.warehouse.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.duckstore.duckstore.warehouse.dto.CreateDuckRequest;
import com.duckstore.duckstore.warehouse.dto.DuckResponse;
import com.duckstore.duckstore.warehouse.dto.UpdateDuckRequest;
import com.duckstore.duckstore.warehouse.service.AddDuckResult;
import com.duckstore.duckstore.warehouse.service.DuckService;
import com.duckstore.duckstore.web.ApiPaths;

import jakarta.validation.Valid;

@RestController
@RequestMapping(ApiPaths.DUCKS)
public class DuckController {

	private final DuckService service;

	public DuckController(DuckService service) {
		this.service = service;
	}

	/** 201 when a new duck record was created, 200 when the quantity was merged into an existing one. */
	@PostMapping
	ResponseEntity<DuckResponse> add(@Valid @RequestBody CreateDuckRequest request) {
		AddDuckResult result = service.add(request);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(result.duck());
	}

	@GetMapping
	List<DuckResponse> list() {
		return service.list();
	}

	@PutMapping(ApiPaths.BY_ID)
	DuckResponse update(@PathVariable int id, @Valid @RequestBody UpdateDuckRequest request) {
		return service.update(id, request);
	}

	@DeleteMapping(ApiPaths.BY_ID)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable int id) {
		service.delete(id);
	}
}
