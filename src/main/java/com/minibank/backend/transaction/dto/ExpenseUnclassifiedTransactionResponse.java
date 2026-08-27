package com.minibank.backend.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ExpenseUnclassifiedTransactionResponse(
	Long transactionId,
	String transactionCode,
	String direction,
	BigDecimal amount,
	String description,
	String counterpartyAccountNumber,
	String counterpartyAccountName,
	String transactionType,
	Instant createdAt
) {}