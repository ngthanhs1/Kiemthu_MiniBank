package com.minibank.backend.support.dto;

import java.time.Instant;

public record ServiceRequestResponse(
	Long id,
	String requestType,
	String title,
	String description,
	String priorityTag,
	String status,
	Instant submittedAt,
	Instant processedAt,
	String processNote
) {}
