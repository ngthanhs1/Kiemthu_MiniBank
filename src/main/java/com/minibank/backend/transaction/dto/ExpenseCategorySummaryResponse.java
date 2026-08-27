package com.minibank.backend.transaction.dto;

import java.math.BigDecimal;

public record ExpenseCategorySummaryResponse(
	String categoryCode,
	String categoryName,
	BigDecimal amount,
	BigDecimal percentage,
	long transactionCount
) {}