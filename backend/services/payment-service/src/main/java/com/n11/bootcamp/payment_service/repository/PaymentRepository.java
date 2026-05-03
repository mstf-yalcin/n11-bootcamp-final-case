package com.n11.bootcamp.payment_service.repository;

import com.n11.bootcamp.payment_service.entity.Payment;
import com.n11.bootcamp.payment_service.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderIdAndIsActiveTrue(UUID orderId);

    boolean existsByOrderIdAndIsActiveTrue(UUID orderId);

    Page<Payment> findAllByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
        SELECT p FROM Payment p
        WHERE p.isActive = true
        AND (CAST(:status AS string) IS NULL OR p.status     = :status)
        AND (CAST(:userId AS string) IS NULL OR p.userId     = :userId)
        AND (CAST(:from   AS string) IS NULL OR p.createdAt >= :from)
        AND (CAST(:to     AS string) IS NULL OR p.createdAt <= :to)
        AND (
            CAST(:pattern AS string) IS NULL OR
            LOWER(p.buyerEmail)     LIKE :pattern OR
            LOWER(p.buyerFirstName) LIKE :pattern OR
            LOWER(p.buyerLastName)  LIKE :pattern OR
            (CAST(:searchUuid AS string) IS NOT NULL AND p.id      = :searchUuid) OR
            (CAST(:searchUuid AS string) IS NOT NULL AND p.orderId = :searchUuid)
        )
    """)
    Page<Payment> searchAdminPayments(
            @Param("status") PaymentStatus status,
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("pattern") String pattern,
            @Param("searchUuid") UUID searchUuid,
            Pageable pageable
    );
}
