package com.minibank.backend.transaction.dto;

import java.math.BigDecimal;

public record ExpenseMonthlyTrendResponse(
	String month,
	String label,
	BigDecimal amount,
	long transactionCount
) {}
