package com.minibank.backend.saving.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSavingRequest(
	@NotNull(message = "Saving product ID is required")
	Long savingProductId,

	@NotNull(message = "Source account ID is required")
	Long sourceAccountId,

	Long settlementAccountId,

	Boolean autoRenew,

	@NotNull(message = "Principal amount is required")
	@Positive(message = "Principal amount must be positive")
	BigDecimal principalAmount
) {}
