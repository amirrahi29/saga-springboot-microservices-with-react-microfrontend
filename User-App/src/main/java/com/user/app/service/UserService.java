package com.user.app.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.user.app.entity.User;
import com.user.app.repository.UserJpaRepository;

@Service
public class UserService {

	private static final Logger log = LoggerFactory.getLogger(UserService.class);

	private final UserJpaRepository users;
	private final PasswordEncoder encoder;

	public UserService(UserJpaRepository users, PasswordEncoder encoder) {
		this.users = users;
		this.encoder = encoder;
	}

	public User register(String name, String email, String password, MultipartFile image) throws IOException {
		log.info("Database lookup for {}", email);
		if (users.findByEmail(email).isPresent()) {
			log.info("Registration failed, email already exists: {}", email);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}
		User user = new User(name, email, encoder.encode(password));
		if (image != null && !image.isEmpty()) {
			user.setImagePath(store(image));
		}
		try {
			User saved = users.save(user);
			log.info("Saved user {} image {}", email, saved.getImagePath());
			return saved;
		}
		catch (DataIntegrityViolationException ex) {
			log.info("Registration failed, email already exists: {}", email);
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}
	}

	private String store(MultipartFile image) throws IOException {
		String type = image.getContentType();
		if (type == null || !type.startsWith("image/")) {
			throw new IOException("not an image");
		}
		String original = image.getOriginalFilename();
		String ext = ".img";
		if (original != null && original.contains(".")) {
			String candidate = original.substring(original.lastIndexOf('.')).toLowerCase();
			if (candidate.matches("\\.(png|jpe?g|gif|webp)")) {
				ext = candidate;
			}
		}
		Path dir = Path.of("uploads");
		Files.createDirectories(dir);
		String filename = UUID.randomUUID() + ext;
		image.transferTo(dir.resolve(filename));
		return "/uploads/" + filename;
	}

	public User authenticate(String email, String password) {
		log.info("Database lookup for {}", email);
		User user = users.findByEmail(email).orElse(null);
		if (user == null || !encoder.matches(password, user.getPassword())) {
			log.info("Authentication failed for {}", email);
			return null;
		}
		log.info("Authentication success for {}", email);
		return user;
	}

	public User findByEmail(String email) {
		log.info("Database lookup for {}", email);
		return users.findByEmail(email).orElse(null);
	}

}
