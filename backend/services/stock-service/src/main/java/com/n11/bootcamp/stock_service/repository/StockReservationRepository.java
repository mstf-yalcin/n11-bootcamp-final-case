package com.n11.bootcamp.stock_service.repository;

import com.n11.bootcamp.stock_service.entity.ReservationStatus;
import com.n11.bootcamp.stock_service.entity.StockReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findAllByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    List<StockReservation> findAllByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);

    @Query("""
        SELECT r FROM StockReservation r
        WHERE (CAST(:status    AS string) IS NULL OR r.status    = :status)
        AND   (CAST(:productId AS string) IS NULL OR r.productId = :productId)
        AND   (CAST(:orderId   AS string) IS NULL OR r.orderId   = :orderId)
    """)
    Page<StockReservation> searchAdminReservations(
            @Param("status") ReservationStatus status,
            @Param("productId") UUID productId,
            @Param("orderId") UUID orderId,
            Pageable pageable
    );
}
