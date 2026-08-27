package com.minibank.backend.transaction.dto;

import java.time.Instant;

public record ExpenseClassifyResponse(
	Long transactionId,
	String categoryCode,
	String categoryName,
	String flowType,
	String source,
	Instant taggedAt
) {}