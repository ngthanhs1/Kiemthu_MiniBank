package com.minibank.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetVerifyRequest(
	@NotBlank String identifier,
	@NotBlank @Size(min = 6, max = 6) String otpCode,
	@NotBlank String newPassword
) {}
