package com.minibank.backend.transaction.dto;

import jakarta.validation.constraints.NotBlank;

public record ExpenseClassifyRequest(
	@NotBlank String categoryCode,
	String flowType,
	String source
) {}