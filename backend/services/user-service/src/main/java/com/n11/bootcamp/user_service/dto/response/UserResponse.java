package com.n11.bootcamp.user_service.dto.response;

import java.util.List;

public record UserResponse(
                String id,
                String email,
                String firstName,
                String lastName,
                List<String> roles) {
}
