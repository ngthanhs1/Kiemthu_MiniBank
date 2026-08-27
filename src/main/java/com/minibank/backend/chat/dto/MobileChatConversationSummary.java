package com.minibank.backend.chat.dto;

import java.time.Instant;

public record MobileChatConversationSummary(
	Long id,
	String status,
	String lastIntent,
	Integer lastConfidence,
	Instant startedAt,
	Instant escalatedAt,
	String lastMessagePreview
) {}
