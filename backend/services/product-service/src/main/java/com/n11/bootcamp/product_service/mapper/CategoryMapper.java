package com.n11.bootcamp.product_service.mapper;

import com.n11.bootcamp.product_service.dto.request.CreateCategoryRequest;
import com.n11.bootcamp.product_service.dto.response.CategoryResponse;
import com.n11.bootcamp.product_service.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    Category toEntity(CreateCategoryRequest request);

    CategoryResponse toResponse(Category category);

    void updateEntity(CreateCategoryRequest request, @MappingTarget Category category);
}
