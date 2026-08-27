package com.minibank.backend.system.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.system.entity.SystemLog;
import com.minibank.backend.system.service.SystemLogService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminAuditLogFilter extends OncePerRequestFilter {
	private static final int MAX_BODY_LENGTH = 2000;

	private final SystemLogService systemLogService;

	public AdminAuditLogFilter(SystemLogService systemLogService) {
		this.systemLogService = systemLogService;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path == null || !path.startsWith("/api/admin/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
		ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
		Instant start = Instant.now();

		int status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		try {
			filterChain.doFilter(wrappedRequest, wrappedResponse);
			status = wrappedResponse.getStatus();
		} finally {
			try {
				logRequest(wrappedRequest, status, Duration.between(start, Instant.now()).toMillis());
			} catch (Exception ignored) {
				// Never block admin requests because of audit logging.
			}
			wrappedResponse.copyBodyToResponse();
		}
	}

	private void logRequest(ContentCachingRequestWrapper request, int status, long durationMs) {
		String path = request.getRequestURI();
		String method = request.getMethod();
		if ("OPTIONS".equalsIgnoreCase(method)) {
			return;
		}

		Long actorId = null;
		String actorType = "ADMIN";
		String subject = null;
		List<String> roles = List.of();
		try {
			Jwt jwt = CurrentJwt.requireJwt();
			actorId = CurrentJwt.requireUserId();
			subject = jwt.getSubject();
			List<String> claimRoles = jwt.getClaimAsStringList("roles");
			if (claimRoles != null) {
				roles = claimRoles;
			}
			String tokenType = CurrentJwt.tokenType();
			if (tokenType != null) {
				actorType = tokenType.toUpperCase();
			}
		} catch (Exception ignored) {
			// Unauthenticated admin endpoints (e.g., login) still get logged.
		}

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		List<String> authorities = List.of();
		if (auth != null && auth.getAuthorities() != null) {
			authorities = auth.getAuthorities().stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());
		}

		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("method", method);
		metadata.put("path", path);
		metadata.put("query", request.getQueryString());
		metadata.put("status", status);
		metadata.put("durationMs", durationMs);
		metadata.put("roles", roles);
		metadata.put("authorities", authorities);
		metadata.put("subject", subject);
		metadata.put("actorId", actorId);
		metadata.put("actorType", actorType);
		metadata.put("userAgent", request.getHeader("User-Agent"));

		String body = extractBody(request);
		if (body != null) {
			metadata.put("body", body);
		}

		String ip = extractIp(request);
		SystemLog log = SystemLog.builder()
			.actorType(actorType)
			.actorId(actorId)
			.action(method + " " + path)
			.targetType(null)
			.targetId(null)
			.metadataJson(systemLogService.toJson(metadata))
			.ipAddress(ip)
			.build();

		systemLogService.log(log);
	}

	private String extractBody(ContentCachingRequestWrapper request) {
		String path = request.getRequestURI();
		if (path != null && path.startsWith("/api/admin/auth/")) {
			return null;
		}
		String contentType = request.getContentType();
		if (contentType == null || !contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE)) {
			return null;
		}
		byte[] buf = request.getContentAsByteArray();
		if (buf == null || buf.length == 0) {
			return null;
		}
		String body = new String(buf, StandardCharsets.UTF_8);
		String masked = maskSensitive(body);
		if (masked.length() > MAX_BODY_LENGTH) {
			return masked.substring(0, MAX_BODY_LENGTH) + "...";
		}
		return masked;
	}

	private String maskSensitive(String body) {
		String masked = body;
		masked = masked.replaceAll("(?i)\\\"password\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\\\"password\\\":\\\"***\\\"");
		masked = masked.replaceAll("(?i)\\\"pin\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\\\"pin\\\":\\\"***\\\"");
		masked = masked.replaceAll("(?i)\\\"otp\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\\\"otp\\\":\\\"***\\\"");
		masked = masked.replaceAll("(?i)\\\"otpCode\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\\\"otpCode\\\":\\\"***\\\"");
		masked = masked.replaceAll("(?i)\\\"transactionPin\\\"\\s*:\\s*\\\"[^\\\"]*\\\"", "\\\"transactionPin\\\":\\\"***\\\"");
		return masked;
	}

	private String extractIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr();
	}
}
