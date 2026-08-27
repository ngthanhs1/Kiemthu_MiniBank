package com.minibank.backend.transaction.dto;

import java.math.BigDecimal;

public record TransferInitiateResponse(
	Long transactionId,
	String transactionCode,
	String status,
	String fromAccountNumber,
	String toAccountNumber,
	String toAccountName,
	BigDecimal amount,
	boolean otpRequired,
	boolean pinRequired,
	String debugOtp
) {}
