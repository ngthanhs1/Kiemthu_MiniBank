package com.minibank.backend.account.dto;

import java.math.BigDecimal;

public record AccountSummaryResponse(
	String accountNumber,
	String accountName,
	BigDecimal availableBalance,
	BigDecimal currentBalance,
	String status,
	String customerRank,
	BigDecimal dailyTransferLimit,
	BigDecimal dailyReceiveLimit
) {}
