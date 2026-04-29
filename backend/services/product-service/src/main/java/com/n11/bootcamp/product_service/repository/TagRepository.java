package com.n11.bootcamp.product_service.repository;

import com.n11.bootcamp.product_service.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findAllByIsActiveTrue();

    Optional<Tag> findBySlugAndIsActiveTrue(String slug);

    boolean existsByNameIgnoreCase(String name);
}
