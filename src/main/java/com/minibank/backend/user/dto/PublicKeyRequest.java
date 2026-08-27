package com.minibank.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record PublicKeyRequest(
	@NotBlank String publicKey
) {}
