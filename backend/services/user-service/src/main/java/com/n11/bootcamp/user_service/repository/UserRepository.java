package com.n11.bootcamp.user_service.repository;

import com.n11.bootcamp.common_lib.auth.enums.Role;
import com.n11.bootcamp.user_service.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.authority = :role AND u.enabled = true")
    long countByRoleAndEnabledTrue(@Param("role") Role role);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN u.roles r
        WHERE (CAST(:pattern AS string) IS NULL OR
               LOWER(u.email)     LIKE :pattern OR
               LOWER(u.firstName) LIKE :pattern OR
               LOWER(u.lastName)  LIKE :pattern)
        AND   (CAST(:role    AS string) IS NULL OR r.authority = :role)
        AND   (CAST(:enabled AS string) IS NULL OR u.enabled   = :enabled)
    """)
    Page<User> searchAdminUsers(
            @Param("pattern") String pattern,
            @Param("role") Role role,
            @Param("enabled") Boolean enabled,
            Pageable pageable
    );
}
