package com.minibank.backend.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferConfirmRequest(
	@NotNull Long transactionId,
	@NotBlank @Size(min = 4, max = 12) String otpCode
) {}
