package com.minibank.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginVerifyRequest(
	@NotBlank String identifier,
	@NotBlank @Size(min = 6, max = 6) String otpCode,
	@NotBlank String deviceId,
	String publicKey
) {}
