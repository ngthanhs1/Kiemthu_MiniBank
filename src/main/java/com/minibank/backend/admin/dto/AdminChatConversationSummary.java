package com.minibank.backend.admin.dto;

import java.time.Instant;

public record AdminChatConversationSummary(
	Long id,
	Long userId,
	String userName,
	String userPhone,
	String customerRank,
	String status,
	String lastIntent,
	Integer lastConfidence,
	Instant startedAt,
	Instant escalatedAt,
	String lastMessagePreview,
	Long assignedAdminUserId,
	String assignedAdminUsername
) {}
