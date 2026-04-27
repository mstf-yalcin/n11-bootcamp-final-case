package com.n11.bootcamp.user_service.repository;

import com.n11.bootcamp.user_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {
    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserIdOrderByExpirationDesc(String userId);

    void deleteByUserId(String userId);
}
