package com.minibank.backend.chat.dto;

import java.time.Instant;
import java.util.List;

public record MobileChatConversationDetailResponse(
	Long conversationId,
	String status,
	String lastIntent,
	Integer lastConfidence,
	Instant startedAt,
	Instant escalatedAt,
	List<MobileChatMessageDto> messages
) {}
