package com.minibank.backend.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminTransactionSummary(
	Long id,
	String transactionCode,
	String fromAccountNumber,
	String fromAccountName,
	String toAccountNumber,
	String toAccountName,
	BigDecimal amount,
	BigDecimal feeAmount,
	String transactionType,
	String status,
	Instant createdAt
) {}
