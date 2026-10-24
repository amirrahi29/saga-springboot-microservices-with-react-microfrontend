package com.user.app.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthorizationFilter extends GenericFilterBean {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

	private final JwtUtil jwtUtil;

	public JwtAuthorizationFilter(JwtUtil jwtUtil) {
		this.jwtUtil = jwtUtil;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		if (request.getRequestURI().startsWith("/api")) {
			String authorization = request.getHeader("Authorization");
			log.info("Request {} {} | Content-Type={} | Authorization={}", request.getMethod(),
					request.getRequestURI(), request.getHeader("Content-Type"),
					authorization == null ? "none" : "Bearer");
			if (authorization != null && authorization.startsWith("Bearer ")) {
				log.info("Extracting access token");
				try {
					String email = jwtUtil.email(authorization.substring(7));
					SecurityContextHolder.getContext().setAuthentication(
							new UsernamePasswordAuthenticationToken(email, null, List.of()));
					log.info("Authentication success for {}", email);
				}
				catch (JwtException | IllegalArgumentException ex) {
					log.info("Authentication failed: {}", ex.getMessage());
				}
			}
		}
		chain.doFilter(request, response);
		if (request.getRequestURI().startsWith("/api")) {
			log.info("Response {} {} -> {}", request.getMethod(), request.getRequestURI(), response.getStatus());
		}
	}

}
