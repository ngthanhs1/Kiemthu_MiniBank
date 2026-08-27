package com.minibank.backend.admin.dto;

import java.time.Instant;
import java.util.List;

public record AdminChatConversationDetail(
	Long id,
	Long userId,
	String userName,
	String userPhone,
	String customerRank,
	String channel,
	String status,
	String lastIntent,
	Integer lastConfidence,
	Instant startedAt,
	Instant escalatedAt,
	Long assignedAdminUserId,
	String assignedAdminUsername,
	List<AdminChatMessageResponse> messages
) {}
