package com.minibank.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PinVerifyRequest(
	@NotBlank @Size(min = 6, max = 6) String pin
) {}
