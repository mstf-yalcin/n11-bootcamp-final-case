package com.n11.bootcamp.user_service.service;

import com.n11.bootcamp.common_lib.auth.enums.Role;
import com.n11.bootcamp.user_service.dto.request.UpdateUserRolesRequest;
import com.n11.bootcamp.user_service.dto.request.UpdateUserStatusRequest;
import com.n11.bootcamp.user_service.dto.response.AdminUserResponse;
import com.n11.bootcamp.user_service.entity.RoleEntity;
import com.n11.bootcamp.user_service.entity.User;
import com.n11.bootcamp.user_service.exception.InvalidAdminOperationException;
import com.n11.bootcamp.user_service.exception.UserNotFoundException;
import com.n11.bootcamp.user_service.repository.RoleRepository;
import com.n11.bootcamp.user_service.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminUserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public Page<AdminUserResponse> search(String search, String role, Boolean isActive, Pageable pageable) {
        String pattern = (search != null && !search.isBlank())
                ? "%" + search.toLowerCase().trim() + "%"
                : null;
        Role roleFilter = parseRole(role);
        return userRepository
                .searchAdminUsers(pattern, roleFilter, isActive, pageable)
                .map(this::toResponse);
    }

    public AdminUserResponse getById(UUID userId) {
        User user = loadUser(userId);
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse updateRoles(UUID userId, UpdateUserRolesRequest request) {
        User user = loadUser(userId);

        Set<Role> targetRoles = new LinkedHashSet<>();
        for (String raw : request.roles()) {
            targetRoles.add(parseRoleStrict(raw));
        }
        if (targetRoles.isEmpty()) {
            throw new InvalidAdminOperationException("At least one role is required");
        }

        boolean wasAdmin = hasRole(user, Role.ADMIN);
        boolean willBeAdmin = targetRoles.contains(Role.ADMIN);

        if (wasAdmin && !willBeAdmin) {
            ensureNotLastAdmin(user);
        }

        List<RoleEntity> entities = targetRoles.stream()
                .map(r -> roleRepository.findByAuthority(r)
                        .orElseThrow(() -> new InvalidAdminOperationException("Role not found: " + r.name())))
                .toList();
        user.setRoles(entities);
        userRepository.save(user);

        log.info("Admin updated roles: userId={}, roles={}", userId, targetRoles);
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse updateStatus(UUID userId, UpdateUserStatusRequest request) {
        User user = loadUser(userId);
        boolean newEnabled = Boolean.TRUE.equals(request.isActive());

        if (!newEnabled && hasRole(user, Role.ADMIN)) {
            ensureNotLastAdmin(user);
        }

        user.setEnabled(newEnabled);
        userRepository.save(user);

        log.info("Admin updated status: userId={}, enabled={}", userId, newEnabled);
        return toResponse(user);
    }

    private User loadUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
    }

    private void ensureNotLastAdmin(User user) {
        long activeAdmins = userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
        boolean userIsActiveAdmin = user.isEnabled() && hasRole(user, Role.ADMIN);
        if (userIsActiveAdmin && activeAdmins <= 1) {
            throw new InvalidAdminOperationException("Cannot remove the last active admin");
        }
    }

    private boolean hasRole(User user, Role role) {
        if (user.getRoles() == null) return false;
        return user.getRoles().stream().anyMatch(r -> r.getRole() == role);
    }

    private Role parseRole(String role) {
        if (role == null || role.isBlank()) return null;
        return parseRoleStrict(role);
    }

    private Role parseRoleStrict(String role) {
        try {
            return Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidAdminOperationException("Unknown role: " + role);
        }
    }

    private AdminUserResponse toResponse(User user) {
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream().map(RoleEntity::getRole).map(Role::name).toList();
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                roles,
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
