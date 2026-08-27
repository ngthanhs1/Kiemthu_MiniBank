package com.minibank.backend.saving.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SavingOpenConfirmRequest(
	@NotNull(message = "transactionId is required")
	Long transactionId,

	@NotBlank(message = "otpCode is required")
	@Size(min = 6, max = 6, message = "otpCode must be 6 digits")
	String otpCode
) {}
