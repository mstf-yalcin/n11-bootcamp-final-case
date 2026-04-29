package com.n11.bootcamp.product_service.controller;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.product_service.dto.request.CreateTagRequest;
import com.n11.bootcamp.product_service.dto.response.TagResponse;
import com.n11.bootcamp.product_service.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Tag management endpoints")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "List all active tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getAllTags() {
        return ResponseEntity.ok(ApiResponse.success(tagService.getAllTags(), "Tags fetched"));
    }

    @PostMapping
    @Operation(summary = "Create a new tag (admin only)")
    public ResponseEntity<ApiResponse<TagResponse>> createTag(@Valid @RequestBody CreateTagRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(tagService.createTag(request), "Tag created"));
    }
}
