package com.minibank.backend.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminAccountSummary(
	Long id,
	String accountNumber,
	String accountName,
	String accountType,
	String currency,
	BigDecimal availableBalance,
	BigDecimal currentBalance,
	BigDecimal dailyTransferLimit,
	BigDecimal dailyReceiveLimit,
	String status,
	Instant openedAt,
	Long ownerId,
	String ownerName,
	String ownerPhone
) {}
