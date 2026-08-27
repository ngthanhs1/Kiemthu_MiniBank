package com.minibank.backend.ai.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AiTransactionClassifyRequest(
	long transactionId,
	String type,
	BigDecimal amount,
	String description,
	String merchant,
	Instant createdAt
) {}
