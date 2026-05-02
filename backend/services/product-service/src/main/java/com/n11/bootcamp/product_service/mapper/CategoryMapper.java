package com.n11.bootcamp.product_service.mapper;

import com.n11.bootcamp.product_service.dto.request.CreateCategoryRequest;
import com.n11.bootcamp.product_service.dto.response.CategoryResponse;
import com.n11.bootcamp.product_service.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "slug", ignore = true)
    Category toEntity(CreateCategoryRequest request);

    CategoryResponse toResponse(Category category);

    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "slug",      ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active",    ignore = true)
    void updateEntity(CreateCategoryRequest request, @MappingTarget Category category);
}
