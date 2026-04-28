package com.n11.bootcamp.common_lib.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageableInfo(
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {
    public static PageableInfo from(Page<?> page) {
        return new PageableInfo(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious()
        );
    }
}
