package com.user.app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String token, String refreshToken, long expiresIn, String imageUrl) {
}
