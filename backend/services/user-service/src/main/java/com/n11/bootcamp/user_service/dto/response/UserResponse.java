package com.n11.bootcamp.user_service.dto.response;

import java.util.List;
import java.util.UUID;

public record UserResponse(
                UUID id,
                String email,
                String firstName,
                String lastName,
                String phone,
                List<String> roles) {
}
