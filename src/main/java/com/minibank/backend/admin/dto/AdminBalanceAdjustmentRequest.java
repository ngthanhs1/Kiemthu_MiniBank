package com.minibank.backend.admin.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminBalanceAdjustmentRequest(
	@NotNull @DecimalMin(value = "0.01") BigDecimal amount,
	@Size(max = 255) String description
) {}
