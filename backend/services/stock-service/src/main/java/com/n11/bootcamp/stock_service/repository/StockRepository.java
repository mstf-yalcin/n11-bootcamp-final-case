package com.n11.bootcamp.stock_service.repository;

import com.n11.bootcamp.stock_service.entity.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Stock, UUID> {

    Optional<Stock> findByProductIdAndIsActiveTrue(UUID productId);

    List<Stock> findAllByIsActiveTrue();

    Page<Stock> findAllByIsActiveTrue(Pageable pageable);

    List<Stock> findAllByProductIdInAndIsActiveTrue(List<UUID> productIds);

    Page<Stock> findAllByProductIdInAndIsActiveTrue(List<UUID> productIds, Pageable pageable);

    boolean existsByProductIdAndIsActiveTrue(UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.productId = :productId AND s.isActive = true")
    Optional<Stock> findByProductIdForUpdate(@Param("productId") UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.productId IN :productIds AND s.isActive = true ORDER BY s.productId")
    List<Stock> findAllByProductIdInForUpdate(@Param("productIds") List<UUID> productIds);

    @Modifying
    @Query("UPDATE Stock s SET s.reserved = s.reserved - :amount WHERE s.productId = :productId AND s.isActive = true AND s.reserved >= :amount")
    int decrementReserved(@Param("productId") UUID productId, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE Stock s SET s.quantity = s.quantity - :amount, s.reserved = s.reserved - :amount WHERE s.productId = :productId AND s.isActive = true AND s.quantity >= :amount AND s.reserved >= :amount")
    int decrementQuantityAndReserved(@Param("productId") UUID productId, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE Stock s SET s.quantity = s.quantity + :amount WHERE s.productId = :productId")
    int incrementQuantity(@Param("productId") UUID productId, @Param("amount") int amount);
}
