package com.duckstore.duckstore.domain.packaging;

import java.util.List;

import com.duckstore.duckstore.domain.model.PackageType;
import com.duckstore.duckstore.domain.model.Protection;

/** The package an order ships in and what fills it. */
public record Packaging(PackageType packageType, List<Protection> protections) {
}
