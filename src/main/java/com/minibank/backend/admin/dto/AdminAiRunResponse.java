package com.minibank.backend.admin.dto;

import java.time.Instant;

public record AdminAiRunResponse(
	String status,
	int processed,
	Instant executedAt
) {}
