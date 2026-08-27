package com.minibank.backend.admin.dto;

import java.time.Instant;

public record AdminChatMessageResponse(
	Long id,
	String senderType,
	Long senderId,
	String messageType,
	String content,
	Instant createdAt
) {}
