package com.minibank.backend.admin.dto;

import java.time.Instant;

public record SystemLogItem(
	Long id,
	String actorType,
	Long actorId,
	String action,
	String targetType,
	Long targetId,
	String metadataJson,
	String ipAddress,
	Instant createdAt
) {}
