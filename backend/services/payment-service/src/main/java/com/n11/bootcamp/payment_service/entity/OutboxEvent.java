package com.n11.bootcamp.payment_service.entity;

import com.n11.bootcamp.common_lib.event.OutboxEventBase;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
        name = "outbox_payment",
        indexes = {
                @Index(name = "idx_outbox_payment_aggregate_type", columnList = "aggregate_type"),
                @Index(name = "idx_outbox_payment_aggregate_id", columnList = "aggregate_id")
        }
)
@SuperBuilder
@NoArgsConstructor
public class OutboxEvent extends OutboxEventBase {}
