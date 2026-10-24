package com.user.app.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.user.app.dto.AuthResponse;
import com.user.app.dto.LoginRequest;
import com.user.app.dto.RefreshRequest;
import com.user.app.dto.UserResponse;
import com.user.app.entity.User;
import com.user.app.mapper.UserMapper;
import com.user.app.security.JwtUtil;
import com.user.app.service.UserService;

import io.jsonwebtoken.JwtException;

@RestController
public class UserController {

	private static final Logger log = LoggerFactory.getLogger(UserController.class);

	private final UserService users;
	private final JwtUtil jwtUtil;
	private final UserMapper mapper;

	public UserController(UserService users, JwtUtil jwtUtil, UserMapper mapper) {
		this.users = users;
		this.jwtUtil = jwtUtil;
		this.mapper = mapper;
	}

	@PostMapping(value = "/api/auth/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<UserResponse> register(@RequestParam String name, @RequestParam String email,
			@RequestParam String password, @RequestParam(required = false) MultipartFile image) {
		if (name.isBlank() || email.isBlank() || password.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, email, and password are required");
		}
		try {
			User saved = users.register(name, email, password, image);
			log.info("Response 201 register {}", email);
			return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(saved));
		}
		catch (IOException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not an image");
		}
	}

	@PostMapping("/api/auth/login")
	public AuthResponse login(@RequestBody LoginRequest body) {
		if (body.email() == null || body.password() == null || body.email().isBlank() || body.password().isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login failed");
		}
		User user = users.authenticate(body.email(), body.password());
		if (user == null) {
			log.info("Response 401 login {}", body.email());
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login failed");
		}
		log.info("Response 200 login {}", body.email());
		return mapper.toAuth(jwtUtil.generateAccessToken(user.getEmail()), jwtUtil.generateRefreshToken(user.getEmail()),
				jwtUtil.getAccessExpiration(), user);
	}

	@PostMapping("/api/auth/refresh")
	public AuthResponse refresh(@RequestBody RefreshRequest body) {
		if (body.refreshToken() == null || body.refreshToken().isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh failed");
		}
		try {
			String email = jwtUtil.emailFromRefresh(body.refreshToken());
			User user = users.findByEmail(email);
			if (user == null) {
				log.info("Response 401 refresh, user missing {}", email);
				throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh failed");
			}
			log.info("Response 200 refresh {}", email);
			return mapper.toAuth(jwtUtil.generateAccessToken(email), jwtUtil.generateRefreshToken(email),
					jwtUtil.getAccessExpiration(), user);
		}
		catch (JwtException | IllegalArgumentException ex) {
			log.info("Response 401 refresh");
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh failed");
		}
	}

	@GetMapping("/api/home")
	public UserResponse home(Authentication authentication) {
		User user = users.findByEmail(authentication.getName());
		if (user == null) {
			log.info("Response 401 home, user missing");
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
		}
		log.info("Response 200 home {}", user.getName());
		return mapper.toResponse(user);
	}

	@GetMapping("/api/saga/users")
	public UserResponse verify(@RequestParam String email) {
		User user = users.findByEmail(email);
		if (user == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
		}
		return mapper.toResponse(user);
	}

}
