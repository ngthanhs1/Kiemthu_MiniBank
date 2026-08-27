package com.minibank.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileChatbotSendRequest(
	Long conversationId,
	Boolean temporary,
	@NotBlank @Size(max = 2000) String message
) {}
