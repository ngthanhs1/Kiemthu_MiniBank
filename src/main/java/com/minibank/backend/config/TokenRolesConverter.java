package com.minibank.backend.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps JWT claim "roles" (array of strings) to Spring authorities.
 */
public class TokenRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

	@Override
	public Collection<GrantedAuthority> convert(Jwt jwt) {
		Object rolesObj = jwt.getClaims().get("roles");
		Object permissionsObj = jwt.getClaims().get("permissions");
		List<String> roles = new ArrayList<>();
		if (rolesObj instanceof List<?> list) {
			for (Object item : list) {
				if (item instanceof String role && !role.isBlank()) {
					roles.add(role.trim());
				}
			}
		} else if (rolesObj instanceof String roleStr && !roleStr.isBlank()) {
			roles.add(roleStr.trim());
		}

		List<GrantedAuthority> authorities = new ArrayList<>();
		for (String role : roles) {
			authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
			if ("SUPER_ADMIN".equalsIgnoreCase(role)) {
				authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
			}
		}

		// Map of legacy (localized) permission labels -> canonical permission codes
		Map<String, String> legacyToCode = Map.ofEntries(
			Map.entry("Duyệt vay", "LOAN_APPLICATION_APPROVAL"),
			Map.entry("Duyệt vay vốn", "LOAN_APPLICATION_APPROVAL"),
			Map.entry("Duyet vay", "LOAN_APPLICATION_APPROVAL"),
			Map.entry("Duyet vay von", "LOAN_APPLICATION_APPROVAL"),
			Map.entry("Duyệt tiết kiệm", "SAVING_APPROVAL"),
			Map.entry("Duyệt vay ", "LOAN_APPLICATION_APPROVAL")
		);

		if (permissionsObj instanceof List<?> list) {
			for (Object item : list) {
				if (item instanceof String permission && !permission.isBlank()) {
					String trimmed = permission.trim();
					authorities.add(new SimpleGrantedAuthority(trimmed));
					String mapped = legacyToCode.get(trimmed);
					if (mapped != null && !mapped.isBlank()) {
						authorities.add(new SimpleGrantedAuthority(mapped));
					}
				}
			}
		} else if (permissionsObj instanceof String permissionStr && !permissionStr.isBlank()) {
			String trimmed = permissionStr.trim();
			authorities.add(new SimpleGrantedAuthority(trimmed));
			String mapped = legacyToCode.get(trimmed);
			if (mapped != null && !mapped.isBlank()) {
				authorities.add(new SimpleGrantedAuthority(mapped));
			}
		}
		return authorities;
	}
}
