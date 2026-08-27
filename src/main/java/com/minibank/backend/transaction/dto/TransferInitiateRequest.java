package com.minibank.backend.transaction.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferInitiateRequest(
	String fromAccountNumber,
	@NotBlank @Size(min = 10, max = 32) String toAccountNumber,
	@NotNull BigDecimal amount,
	@Size(max = 500) String description,
	@Size(max = 128) String idempotencyKey,
	Long qrTransferIntentId,
	@NotBlank String signature,
	@NotBlank @Size(min = 6, max = 6) String pin
) {}
