package com.n11.bootcamp.order_service.repository;

import com.n11.bootcamp.common_lib.event.order.OrderStatus;
import com.n11.bootcamp.order_service.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndIsActiveTrue(UUID id);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserIdAndIsActiveTrue(UUID id, UUID userId);

    Page<Order> findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
        SELECT o FROM Order o
        WHERE o.isActive = true
        AND (CAST(:status     AS string) IS NULL OR o.status     = :status)
        AND (CAST(:userId     AS string) IS NULL OR o.userId     = :userId)
        AND (CAST(:from       AS string) IS NULL OR o.createdAt >= :from)
        AND (CAST(:to         AS string) IS NULL OR o.createdAt <= :to)
        AND (
            CAST(:pattern AS string) IS NULL OR
            LOWER(o.buyerEmail)     LIKE :pattern OR
            LOWER(o.buyerFirstName) LIKE :pattern OR
            LOWER(o.buyerLastName)  LIKE :pattern OR
            (CAST(:searchUuid AS string) IS NOT NULL AND o.id = :searchUuid)
        )
    """)
    Page<Order> searchAdminOrders(
            @Param("status") OrderStatus status,
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("pattern") String pattern,
            @Param("searchUuid") UUID searchUuid,
            Pageable pageable
    );
}
