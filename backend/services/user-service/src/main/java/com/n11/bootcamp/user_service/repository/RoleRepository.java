package com.n11.bootcamp.user_service.repository;

import com.n11.bootcamp.user_service.entity.RoleEntity;
import com.n11.bootcamp.user_service.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {
    Optional<RoleEntity> findByAuthority(Role authority);
}
