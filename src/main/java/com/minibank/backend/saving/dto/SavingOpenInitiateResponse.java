package com.minibank.backend.saving.dto;

public record SavingOpenInitiateResponse(
	Long transactionId,
	String transactionCode,
	String status,
	boolean otpRequired,
	String debugOtp
) {}
