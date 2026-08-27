package com.minibank.backend.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminLedgerEntryResponse(
	Long id,
	String accountNumber,
	String accountName,
	String entryType,
	BigDecimal amount,
	BigDecimal balanceBefore,
	BigDecimal balanceAfter,
	String transactionCode,
	Instant createdAt
) {}
