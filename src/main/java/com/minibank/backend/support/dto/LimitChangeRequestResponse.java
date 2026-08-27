package com.minibank.backend.support.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LimitChangeRequestResponse(
	Long id,
	Long serviceRequestId,
	Long accountId,
	String accountNumber,
	String accountName,
	BigDecimal currentDailyTransferLimit,
	BigDecimal requestedDailyTransferLimit,
	String reason,
	String status,
	Instant submittedAt,
	Instant processedAt,
	String processNote
) {}
