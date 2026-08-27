package com.minibank.backend.transaction.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferConfirmResponse(
	Long transactionId,
	String status,
	Instant completedAt,
	String fromAccountNumber,
	String toAccountNumber,
	BigDecimal amount,
	BigDecimal fromAvailableBalance
) {}
