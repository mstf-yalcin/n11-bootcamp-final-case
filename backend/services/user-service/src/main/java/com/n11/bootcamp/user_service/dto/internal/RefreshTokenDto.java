package com.n11.bootcamp.user_service.dto.internal;

import com.n11.bootcamp.user_service.entity.User;

public record RefreshTokenDto(String rawToken, User user) {
}
