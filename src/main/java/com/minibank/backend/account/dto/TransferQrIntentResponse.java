package com.minibank.backend.account.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferQrIntentResponse(
	Long intentId,
	String intentToken,
	String accountNumber,
	String accountName,
	BigDecimal amount,
	String status,
	String payload,
	Instant expiresAt,
	Instant claimedAt,
	Instant completedAt
) {}