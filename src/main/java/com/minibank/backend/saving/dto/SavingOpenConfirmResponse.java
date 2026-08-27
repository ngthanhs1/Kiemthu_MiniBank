package com.minibank.backend.saving.dto;

public record SavingOpenConfirmResponse(
	Long transactionId,
	String status,
	Long savingId,
	String savingCode
) {}
