package com.n11.bootcamp.order_service.repository;

import com.n11.bootcamp.order_service.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {}
