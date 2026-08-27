package com.minibank.backend.admin.dto;

import java.time.Instant;
import java.util.List;

public record AdminStaffResponse(
	Long id,
	String username,
	String email,
	String fullName,
	String status,
	List<String> roles,
	Instant createdAt
) {}
