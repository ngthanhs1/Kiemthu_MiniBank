package com.minibank.backend.admin.dto;

public record UserSummary(
	Long id,
	String phone,
	String email,
	String fullName,
	String status,
	String customerRank,
	String deviceId
) {}
