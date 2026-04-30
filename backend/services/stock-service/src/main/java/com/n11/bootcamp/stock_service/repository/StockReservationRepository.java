package com.n11.bootcamp.stock_service.repository;

import com.n11.bootcamp.stock_service.entity.ReservationStatus;
import com.n11.bootcamp.stock_service.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findAllByOrderIdAndStatus(UUID orderId, ReservationStatus status);

    List<StockReservation> findAllByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);
}
