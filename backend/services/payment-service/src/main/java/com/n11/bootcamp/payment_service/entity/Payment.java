package com.n11.bootcamp.payment_service.entity;

import com.n11.bootcamp.common_lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_order_id", columnList = "order_id", unique = true),
                @Index(name = "idx_payments_user_id", columnList = "user_id"),
                @Index(name = "idx_payments_status", columnList = "status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Payment extends BaseEntity {

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    // Nullable: STOCK_RESERVED may arrive before ORDER_CREATED — userId is filled by ORDER_CREATED handler.
    @Column(name = "user_id")
    private UUID userId;

    // Nullable for the same reason as userId.
    @Column(name = "amount", precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "TRY";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "stock_confirmed", nullable = false)
    @Builder.Default
    private boolean stockConfirmed = false;

    @Column(name = "provider", nullable = false, length = 30)
    @Builder.Default
    private String provider = "IYZICO";

    @Column(name = "provider_payment_id", length = 100)
    private String providerPaymentId;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    // ===== Buyer snapshot (self-contained — Iyzico request + audit + refund) =====
    @Column(name = "buyer_first_name", length = 100)
    private String buyerFirstName;

    @Column(name = "buyer_last_name", length = 100)
    private String buyerLastName;

    @Column(name = "buyer_email", length = 255)
    private String buyerEmail;

    @Column(name = "buyer_phone", length = 20)
    private String buyerPhone;

    @Column(name = "buyer_identity_number", length = 11)
    private String buyerIdentityNumber;

    @Column(name = "buyer_ip", length = 45)
    private String buyerIp;

    // ===== Shipping snapshot =====
    @Column(name = "shipping_contact_name", length = 100)
    private String shippingContactName;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "shipping_city", length = 100)
    private String shippingCity;

    @Column(name = "shipping_district", length = 100)
    private String shippingDistrict;

    @Column(name = "shipping_country", length = 100)
    private String shippingCountry;

    @Column(name = "shipping_zip_code", length = 10)
    private String shippingZipCode;

    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
