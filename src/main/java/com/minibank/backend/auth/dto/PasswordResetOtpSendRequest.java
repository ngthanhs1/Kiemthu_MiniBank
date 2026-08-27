package com.minibank.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordResetOtpSendRequest(
	@NotBlank String identifier
) {}
