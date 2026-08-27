package com.minibank.backend.account.dto;

import jakarta.validation.constraints.NotBlank;

public record ClaimTransferQrRequest(
	@NotBlank String intentToken
) {}