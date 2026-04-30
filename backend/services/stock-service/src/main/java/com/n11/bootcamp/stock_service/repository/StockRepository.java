package com.n11.bootcamp.stock_service.repository;

import com.n11.bootcamp.stock_service.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockRepository extends JpaRepository<Inventory, UUID> {

    Optional<Inventory> findByProductIdAndIsActiveTrue(UUID productId);

    List<Inventory> findAllByIsActiveTrue();

    boolean existsByProductIdAndIsActiveTrue(UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId AND i.isActive = true")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") UUID productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId IN :productIds AND i.isActive = true ORDER BY i.productId")
    List<Inventory> findAllByProductIdInForUpdate(@Param("productIds") List<UUID> productIds);

    @Modifying
    @Query("UPDATE Inventory i SET i.reserved = i.reserved - :amount WHERE i.productId = :productId AND i.isActive = true AND i.reserved >= :amount")
    int decrementReserved(@Param("productId") UUID productId, @Param("amount") int amount);

    @Modifying
    @Query("UPDATE Inventory i SET i.quantity = i.quantity - :amount, i.reserved = i.reserved - :amount WHERE i.productId = :productId AND i.isActive = true AND i.quantity >= :amount AND i.reserved >= :amount")
    int decrementQuantityAndReserved(@Param("productId") UUID productId, @Param("amount") int amount);
}
