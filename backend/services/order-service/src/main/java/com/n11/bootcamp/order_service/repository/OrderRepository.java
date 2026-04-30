package com.n11.bootcamp.order_service.repository;

import com.n11.bootcamp.order_service.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndIsActiveTrue(UUID id);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserIdAndIsActiveTrue(UUID id, UUID userId);

    Page<Order> findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
