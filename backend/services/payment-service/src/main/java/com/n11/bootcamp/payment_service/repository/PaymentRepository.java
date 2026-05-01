package com.n11.bootcamp.payment_service.repository;

import com.n11.bootcamp.payment_service.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderIdAndIsActiveTrue(UUID orderId);

    boolean existsByOrderIdAndIsActiveTrue(UUID orderId);

    Page<Payment> findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
