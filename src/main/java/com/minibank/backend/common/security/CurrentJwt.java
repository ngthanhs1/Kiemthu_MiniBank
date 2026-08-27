package com.minibank.backend.common.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

public final class CurrentJwt {
	private CurrentJwt() {}

	public static Jwt requireJwt() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authentication");
		}
		return jwt;
	}

	public static long requireUserId() {
		Jwt jwt = requireJwt();
		Object uid = jwt.getClaims().get("uid");
		if (uid instanceof Number n) {
			return n.longValue();
		}
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
	}

	public static String tokenType() {
		Jwt jwt = requireJwt();
		Object type = jwt.getClaims().get("type");
		return type == null ? null : type.toString();
	}
}
