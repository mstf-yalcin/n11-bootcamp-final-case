package com.n11.bootcamp.product_service.repository;

import com.n11.bootcamp.product_service.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findByIdAndIsActiveTrue(UUID id);

    Optional<Product> findBySlugAndIsActiveTrue(String slug);

    boolean existsBySlug(String slug);

    List<Product> findAllByIdInAndIsActiveTrue(List<UUID> ids);

    @Query("""
            SELECT p FROM Product p
            WHERE p.isActive = true
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:minPrice IS NULL OR p.price >= :minPrice)
              AND (:maxPrice IS NULL OR p.price <= :maxPrice)
              AND (:search IS NULL
                   OR LOWER(p.name) LIKE :search
                   OR LOWER(p.description) LIKE :search)
            """)
    Page<Product> findWithFilters(
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("search") String search,
            Pageable pageable
    );
}
