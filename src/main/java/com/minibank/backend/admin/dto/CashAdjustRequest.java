package com.minibank.backend.admin.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CashAdjustRequest(
	@NotNull BigDecimal amount,
	@Size(max = 500) String reason
) {}
