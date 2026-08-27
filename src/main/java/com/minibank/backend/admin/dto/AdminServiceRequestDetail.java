package com.minibank.backend.admin.dto;

import java.time.Instant;

import com.minibank.backend.support.dto.LimitChangeRequestResponse;

public record AdminServiceRequestDetail(
	Long id,
	String requestType,
	String title,
	String description,
	String payloadJson,
	String status,
	String priorityTag,
	Instant submittedAt,
	Instant processedAt,
	String processNote,
	Long userId,
	String userName,
	String userPhone,
	LimitChangeRequestResponse limitChange
) {}
