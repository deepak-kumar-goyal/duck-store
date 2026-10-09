package com.duckstore.duckstore.warehouse.service;

import com.duckstore.duckstore.warehouse.dto.DuckResponse;

/** {@code created} is true when a new record was inserted, false when the quantity was merged into an existing one. */
public record AddDuckResult(DuckResponse duck, boolean created) {
}
