package com.minibank.backend.chat.dto;

import java.time.Instant;

public record ChatConversationEvent(
	Long conversationId,
	Long userId,
	String customerName,
	String customerPhone,
	String customerRank,
	String status,
	Instant startedAt,
	Instant escalatedAt,
	Long assignedAdminUserId,
	String assignedAdminUsername,
	String lastMessagePreview
) {}
