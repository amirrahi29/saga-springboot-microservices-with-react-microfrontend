package com.user.app.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

	private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

	private final SecretKey key = Keys.hmacShaKeyFor(
			"saga-user-app-jwt-secret-key-32b".getBytes(StandardCharsets.UTF_8));
	private final long accessExpiration;
	private final long refreshExpiration;

	public JwtUtil(@Value("${jwt.access-expiration}") long accessExpiration,
			@Value("${jwt.refresh-expiration}") long refreshExpiration) {
		this.accessExpiration = accessExpiration;
		this.refreshExpiration = refreshExpiration;
	}

	public long getAccessExpiration() {
		return accessExpiration;
	}

	public String generateAccessToken(String email) {
		log.info("Generated access token for {} expiring in {} ms", email, accessExpiration);
		return Jwts.builder()
				.subject(email)
				.claim("type", "access")
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + accessExpiration))
				.signWith(key)
				.compact();
	}

	public String generateRefreshToken(String email) {
		log.info("Generated refresh token for {} expiring in {} ms", email, refreshExpiration);
		return Jwts.builder()
				.subject(email)
				.claim("type", "refresh")
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + refreshExpiration))
				.signWith(key)
				.compact();
	}

	public Date extractExpiration(String token) {
		return claims(token).getExpiration();
	}

	public boolean isTokenExpired(String token) {
		try {
			return extractExpiration(token).before(new Date());
		}
		catch (ExpiredJwtException ex) {
			return true;
		}
	}

	public String validateAccessToken(String token) {
		if (isTokenExpired(token)) {
			throw new JwtException("access token expired");
		}
		Claims claims = claims(token);
		if (!"access".equals(claims.get("type", String.class))) {
			throw new JwtException("wrong token type");
		}
		return claims.getSubject();
	}

	public String email(String token) {
		return validateAccessToken(token);
	}

	public String emailFromRefresh(String token) {
		if (isTokenExpired(token)) {
			throw new JwtException("refresh token expired");
		}
		Claims claims = claims(token);
		if (!"refresh".equals(claims.get("type", String.class))) {
			throw new JwtException("wrong token type");
		}
		return claims.getSubject();
	}

	private Claims claims(String token) {
		return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
	}

}
