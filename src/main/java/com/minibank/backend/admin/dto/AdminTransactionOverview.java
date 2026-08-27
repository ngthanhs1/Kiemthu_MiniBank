package com.minibank.backend.admin.dto;

import java.math.BigDecimal;

public record AdminTransactionOverview(
	long totalTransactions,
	long completedTransactions,
	BigDecimal totalCompletedAmount
) {}
