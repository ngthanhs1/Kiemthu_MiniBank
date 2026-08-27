package com.minibank.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatSendRequest(
	@NotNull Long conversationId,
	@NotBlank String senderType,
	Long senderId,
	@NotBlank String content
) {}
