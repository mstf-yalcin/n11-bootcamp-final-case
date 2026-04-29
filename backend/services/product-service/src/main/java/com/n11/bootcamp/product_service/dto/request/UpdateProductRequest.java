package com.n11.bootcamp.product_service.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record UpdateProductRequest(
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        @Size(max = 2000, message = "Description must not exceed 2000 characters")
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
        BigDecimal price,

        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO 4217 code (e.g. TRY, USD, EUR)")
        String currency,

        @URL(message = "Image URL must be valid")
        @NotBlank(message = "Image url is required")
        String imageUrl,

        Set<UUID> tagIds,

        @NotNull(message = "Category is required")
        UUID categoryId
) {
}
