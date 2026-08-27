package com.minibank.backend.chat.dto;

import jakarta.validation.constraints.NotNull;

public record ChatTypingRequest(
	@NotNull Long conversationId,
	String senderType,
	Long senderId,
	boolean typing
) {}
