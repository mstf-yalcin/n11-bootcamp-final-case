package com.n11.bootcamp.product_service.repository;

import com.n11.bootcamp.product_service.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findAllByIsActiveTrue();

    Optional<Category> findByIdAndIsActiveTrue(UUID id);

    Optional<Category> findBySlugAndIsActiveTrue(String slug);

    boolean existsByNameAndIsActiveTrue(String name);

    boolean existsBySlug(String slug);
}
