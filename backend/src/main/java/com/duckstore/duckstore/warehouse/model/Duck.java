package com.duckstore.duckstore.warehouse.model;

import java.math.BigDecimal;

import com.duckstore.duckstore.domain.model.Color;
import com.duckstore.duckstore.domain.model.Size;
import com.duckstore.duckstore.domain.pricing.Money;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "duck")
public class Duck {

	/** Largest stock a single duck record can hold. Mirrored by a CHECK constraint in the database. */
	public static final int MAX_QUANTITY = 1_000_000;

	/** Price precision, shared by the column, the request validation and the money rounding (DECIMAL(10,2)). */
	public static final int PRICE_INTEGER_DIGITS = 8;
	public static final int PRICE_FRACTION_DIGITS = Money.SCALE;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Color color;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Size size;

	/**
	 * DECISION: the requirements list Price as a Double. It is part of the "same duck" key and is money, so it is a
	 * BigDecimal with 2 decimals (DECIMAL(10,2)): doubles cannot be compared or summed exactly.
	 */
	@Column(nullable = false, precision = PRICE_INTEGER_DIGITS + PRICE_FRACTION_DIGITS, scale = PRICE_FRACTION_DIGITS)
	private BigDecimal price;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false)
	private boolean deleted;

	@Version
	private long version;

	protected Duck() {
	}

	public Duck(Color color, Size size, BigDecimal price, int quantity) {
		this.color = color;
		this.size = size;
		this.price = price;
		this.quantity = quantity;
	}

	public Integer getId() {
		return id;
	}

	public Color getColor() {
		return color;
	}

	public Size getSize() {
		return size;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public int getQuantity() {
		return quantity;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void update(BigDecimal price, int quantity) {
		this.price = price;
		this.quantity = quantity;
	}

	public void markDeleted() {
		this.deleted = true;
	}
}
