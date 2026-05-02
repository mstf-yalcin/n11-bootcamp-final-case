package com.n11.bootcamp.common_lib.auth.enums;

public enum Role {

    USER,
    ADMIN;

    public String getAuthority() {
        return "ROLE_" + name();
    }
}
