package com.minibank.backend.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminTransactionClassificationResponse(
	long transactionId,
	String transactionCode,
	Instant createdAt,
	String customerName,
	String accountNumber,
	String description,
	BigDecimal amount,
	String categoryCode,
	String categoryName,
	BigDecimal confidence,
	String source,
	String verificationStatus
) {}
