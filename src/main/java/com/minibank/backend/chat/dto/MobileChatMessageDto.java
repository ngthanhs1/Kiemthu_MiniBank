package com.minibank.backend.chat.dto;

import java.time.Instant;

public record MobileChatMessageDto(
	Long id,
	String senderType,
	String messageType,
	String content,
	Instant createdAt
) {}
