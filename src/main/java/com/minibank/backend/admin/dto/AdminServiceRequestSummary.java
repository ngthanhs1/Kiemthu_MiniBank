package com.minibank.backend.admin.dto;

import java.time.Instant;

public record AdminServiceRequestSummary(
	Long id,
	String requestType,
	String title,
	String status,
	String priorityTag,
	Instant submittedAt,
	Long userId,
	String userName,
	String userPhone
) {}
