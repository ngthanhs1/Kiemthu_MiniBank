package com.minibank.backend.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminPendingLargeTransactionResponse(
	Long id,
	String transactionCode,
	String fromAccountNumber,
	String fromAccountName,
	String toAccountNumber,
	String toAccountName,
	BigDecimal amount,
	String status,
	String riskLevel,
	Instant createdAt
) {}
