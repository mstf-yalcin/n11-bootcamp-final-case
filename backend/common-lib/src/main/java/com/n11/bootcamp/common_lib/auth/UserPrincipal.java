package com.n11.bootcamp.common_lib.auth;

import java.util.List;

public record UserPrincipal(String id, String email, List<String> roles) {

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
