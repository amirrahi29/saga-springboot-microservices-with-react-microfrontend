package com.user.app.mapper;

import org.springframework.stereotype.Component;

import com.user.app.dto.AuthResponse;
import com.user.app.dto.UserResponse;
import com.user.app.entity.User;

@Component
public class UserMapper {

	public UserResponse toResponse(User user) {
		return new UserResponse(user.getName(), user.getImagePath());
	}

	public AuthResponse toAuth(String token, String refreshToken, long expiresIn, User user) {
		return new AuthResponse(token, refreshToken, expiresIn, user == null ? null : user.getImagePath());
	}

}
