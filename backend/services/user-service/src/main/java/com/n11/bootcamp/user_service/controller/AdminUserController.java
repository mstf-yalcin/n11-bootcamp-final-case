package com.n11.bootcamp.user_service.controller;

import com.n11.bootcamp.common_lib.dto.response.ApiResponse;
import com.n11.bootcamp.user_service.dto.request.UpdateUserRolesRequest;
import com.n11.bootcamp.user_service.dto.request.UpdateUserStatusRequest;
import com.n11.bootcamp.user_service.dto.response.AdminUserResponse;
import com.n11.bootcamp.user_service.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "Admin endpoints for managing users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    @Operation(summary = "List users with filters (admin only)")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<AdminUserResponse> page = adminUserService.search(search, role, isActive, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id (admin only)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getById(id), "User fetched"));
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "Update user roles (admin only)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateRoles(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminUserService.updateRoles(id, request), "User roles updated"));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a user (admin only)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                adminUserService.updateStatus(id, request), "User status updated"));
    }
}
