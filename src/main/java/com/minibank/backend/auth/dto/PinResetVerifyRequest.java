package com.minibank.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PinResetVerifyRequest(
	@NotBlank @Size(min = 6, max = 6) String otpCode,
	@NotBlank @Size(min = 6, max = 6) String newPin
) {}
