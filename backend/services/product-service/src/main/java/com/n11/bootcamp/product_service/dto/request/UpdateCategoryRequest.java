package com.n11.bootcamp.product_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateCategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must not exceed 100 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotBlank(message = "Image url is required")
        @URL(message = "Image URL must be valid")
        @Size(max = 1024, message = "Image URL must not exceed 1024 characters")
        String imageUrl
) {
}
