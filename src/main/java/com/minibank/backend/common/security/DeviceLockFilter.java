package com.minibank.backend.common.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DeviceLockFilter extends OncePerRequestFilter {
	private final UserRepository userRepository;

	public DeviceLockFilter(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path.startsWith("/api/mobile/auth/") || path.startsWith("/api/admin/auth/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		Jwt jwt;
		try {
			jwt = CurrentJwt.requireJwt();
		} catch (Exception e) {
			filterChain.doFilter(request, response);
			return;
		}

		String type = CurrentJwt.tokenType();
		if (!"USER".equalsIgnoreCase(type)) {
			filterChain.doFilter(request, response);
			return;
		}

		String tokenDeviceId = jwt.getClaimAsString("deviceId");
		if (tokenDeviceId == null || tokenDeviceId.isBlank()) {
			reject(response, "Session expired");
			return;
		}

		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId).orElse(null);
		if (user == null || user.getDeviceId() == null || user.getDeviceId().isBlank()) {
			reject(response, "Session expired");
			return;
		}

		if (!tokenDeviceId.equals(user.getDeviceId())) {
			reject(response, "Account is active on another device");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private static void reject(HttpServletResponse response, String message) throws IOException {
		response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"error\":\"" + message + "\"}");
	}
}
