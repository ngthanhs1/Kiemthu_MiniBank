package com.minibank.backend.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MobileTransactionSummary(
	Long id,
	String direction,
	BigDecimal amount,
	String description,
	String counterpartyAccountNumber,
	String counterpartyName,
	String transactionType,
	String status,
	Instant createdAt,
	String categoryCode,
	String categorySource,
	BigDecimal categoryConfidence
) {}
