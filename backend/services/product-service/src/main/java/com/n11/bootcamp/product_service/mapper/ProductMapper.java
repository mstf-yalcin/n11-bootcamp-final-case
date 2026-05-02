package com.n11.bootcamp.product_service.mapper;

import com.n11.bootcamp.product_service.dto.request.CreateProductRequest;
import com.n11.bootcamp.product_service.dto.request.UpdateProductRequest;
import com.n11.bootcamp.product_service.dto.response.ProductResponse;
import com.n11.bootcamp.product_service.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = {TagMapper.class})
public interface ProductMapper {

    @Mapping(target = "category",      ignore = true)
    @Mapping(target = "slug",          ignore = true)
    @Mapping(target = "currency",      ignore = true)
    @Mapping(target = "ratingCount",   ignore = true)
    @Mapping(target = "ratingAverage", ignore = true)
    @Mapping(target = "tags",          ignore = true)
    Product toEntity(CreateProductRequest request);

    @Mapping(source = "category.id",   target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(target = "stockStatus",      ignore = true)
    @Mapping(target = "availableQuantity", ignore = true)
    ProductResponse toResponse(Product product);

    @Mapping(target = "category",      ignore = true)
    @Mapping(target = "slug",          ignore = true)
    @Mapping(target = "currency",      ignore = true)
    @Mapping(target = "ratingCount",   ignore = true)
    @Mapping(target = "ratingAverage", ignore = true)
    @Mapping(target = "tags",          ignore = true)
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    @Mapping(target = "active",        ignore = true)
    void updateEntity(UpdateProductRequest request, @MappingTarget Product product);
}
