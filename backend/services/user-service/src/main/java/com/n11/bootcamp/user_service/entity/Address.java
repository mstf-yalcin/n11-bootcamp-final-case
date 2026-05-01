package com.n11.bootcamp.user_service.entity;

import com.n11.bootcamp.common_lib.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Entity
@Table(
        name = "addresses",
        indexes = {
                @Index(name = "idx_addresses_user_id", columnList = "user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class Address extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "title", nullable = false, length = 50)
    private String title;

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "full_address", nullable = false, length = 500)
    private String fullAddress;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "country", nullable = false, length = 100)
    @Builder.Default
    private String country = "Turkey";

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "phone", length = 15)
    private String phone;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;
}
