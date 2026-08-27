package com.minibank.backend.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LargeTransactionSummary(
	Long id,
	String transactionCode,
	String fromName,
	String fromAccountNumber,
	String toName,
	String toAccountNumber,
	BigDecimal amount,
	String currency,
	String riskLevel,
	String reviewStatus,
	Instant createdAt
) {}
