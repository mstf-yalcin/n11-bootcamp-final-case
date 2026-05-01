package com.n11.bootcamp.user_service.repository;

import com.n11.bootcamp.user_service.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findAllByUserIdAndIsActiveTrueOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    Optional<Address> findByIdAndUserIdAndIsActiveTrue(UUID id, UUID userId);

    boolean existsByUserIdAndIsDefaultTrueAndIsActiveTrue(UUID userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId AND a.isDefault = true AND a.id <> :exceptId")
    int unsetDefaultExcept(@Param("userId") UUID userId, @Param("exceptId") UUID exceptId);
}
