package com.minibank.backend.support.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateLimitChangeRequest(
	@NotNull(message = "Account ID is required")
	Long accountId,

	@NotNull(message = "Requested daily transfer limit is required")
	@Positive(message = "Requested daily transfer limit must be positive")
	BigDecimal requestedDailyTransferLimit,

	String reason
) {}
