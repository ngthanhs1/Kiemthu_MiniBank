package com.minibank.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginOtpSendRequest(
	@NotBlank String identifier,
	@NotBlank String password,
	@NotBlank String deviceId
) {}
