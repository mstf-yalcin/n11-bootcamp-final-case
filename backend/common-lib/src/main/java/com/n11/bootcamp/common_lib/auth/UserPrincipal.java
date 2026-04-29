package com.n11.bootcamp.common_lib.auth;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.util.List;

public record UserPrincipal(String id, String email, List<String> roles)
        implements AuthenticatedPrincipal {

    @Override
    public String getName() {
        return email;
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
