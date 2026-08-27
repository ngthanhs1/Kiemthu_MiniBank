package com.minibank.backend.auth.dto;

import java.util.List;

public record AuthResponse(
	String tokenType,
	String accessToken,
	long expiresInSeconds,
	UserInfo user
) {
	public record UserInfo(
		Long id,
		String type,
		String username,
		String phone,
		List<String> roles,
		List<String> permissions
	) {}
}
