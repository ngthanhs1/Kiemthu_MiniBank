package com.minibank.backend.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MobileChatTranscriptMessage(
	@NotBlank String senderType,
	String messageType,
	@NotBlank @Size(max = 2000) String content
) {}
