package com.minibank.backend.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LargeTransactionDetail(
	Long id,
	String transactionCode,
	String transactionType,
	String fromName,
	String fromAccountNumber,
	String toName,
	String toAccountNumber,
	BigDecimal amount,
	BigDecimal feeAmount,
	String description,
	String currency,
	String riskLevel,
	String reviewStatus,
	String status,
	Instant createdAt,
	Instant completedAt
) {}
