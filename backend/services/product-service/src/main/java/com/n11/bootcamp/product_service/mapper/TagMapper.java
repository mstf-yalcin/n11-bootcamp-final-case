package com.n11.bootcamp.product_service.mapper;

import com.n11.bootcamp.product_service.dto.response.TagResponse;
import com.n11.bootcamp.product_service.entity.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponse toResponse(Tag tag);
}
