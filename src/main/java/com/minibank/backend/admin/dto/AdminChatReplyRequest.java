package com.minibank.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminChatReplyRequest(
	@NotBlank @Size(max = 2000) String message
) {}
