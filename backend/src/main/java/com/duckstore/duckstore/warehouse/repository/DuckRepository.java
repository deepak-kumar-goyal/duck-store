package com.duckstore.duckstore.warehouse.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.duckstore.duckstore.domain.model.Color;
import com.duckstore.duckstore.domain.model.Size;
import com.duckstore.duckstore.warehouse.model.Duck;

public interface DuckRepository extends JpaRepository<Duck, Integer> {

	/**
	 * DECISION: the requirements say "sort by quantity" without a direction. Ascending (lowest stock first) was
	 * chosen so running-low items are on top; ties are ordered by id so the order is stable.
	 */
	List<Duck> findByDeletedFalseOrderByQuantityAscIdAsc();

	Optional<Duck> findByIdAndDeletedFalse(Integer id);

	Optional<Duck> findByColorAndSizeAndPrice(Color color, Size size, BigDecimal price);

	boolean existsByColorAndSizeAndPriceAndIdNot(Color color, Size size, BigDecimal price, Integer id);

	boolean existsByColorAndSizeAndDeletedFalse(Color color, Size size);

	@Query("""
			select d from Duck d
			where d.color = :color and d.size = :size and d.deleted = false and d.quantity >= :quantity
			order by d.price asc, d.id asc
			limit 1
			""")
	Optional<Duck> findCheapestInStock(@Param("color") Color color, @Param("size") Size size,
			@Param("quantity") int quantity);

	/**
	 * Atomically takes stock: succeeds only while the duck is active and still has enough units, so two orders can
	 * never take the same units. The version is bumped so concurrent edits are detected.
	 *
	 * @return 1 when the stock was taken, 0 when there was not enough left
	 */
	@Modifying
	@Query("""
			update Duck d set d.quantity = d.quantity - :quantity, d.version = d.version + 1
			where d.id = :id and d.deleted = false and d.quantity >= :quantity
			""")
	int takeStock(@Param("id") Integer id, @Param("quantity") int quantity);

	/**
	 * Single atomic statement: inserts a new duck or adds to the existing one. A logically deleted duck with the
	 * same color, size and price is revived with exactly the new quantity. The version is bumped so concurrent
	 * edits of the same duck are detected.
	 * <p>
	 * DECISION (merge invariant): the requirements demand that "same price, color and size" never produces two
	 * records, even for simultaneous requests. A read-then-write in Java cannot guarantee that, so the unique key
	 * plus this single statement make the database the arbiter.
	 * <p>
	 * DECISION (deleted duck): the requirements do not say what adding a duck that matches a logically deleted one
	 * should do. Because deleted rows keep occupying the unique key, the row is revived instead of creating a
	 * duplicate, and the stock that was deleted is not resurrected: only the newly added quantity counts.
	 *
	 * @return 1 when a new row was inserted, 2 when an existing row was updated (MySQL's affected-rows semantics)
	 */
	@Modifying
	@Query(nativeQuery = true, value = """
			INSERT INTO duck (color, size, price, quantity, deleted)
			VALUES (:#{#color.name()}, :#{#size.name()}, :price, :quantity, FALSE) AS incoming
			ON DUPLICATE KEY UPDATE
			    quantity = IF(duck.deleted, incoming.quantity, duck.quantity + incoming.quantity),
			    deleted = FALSE,
			    version = duck.version + 1
			""")
	int upsert(@Param("color") Color color, @Param("size") Size size, @Param("price") BigDecimal price,
			@Param("quantity") int quantity);
}
