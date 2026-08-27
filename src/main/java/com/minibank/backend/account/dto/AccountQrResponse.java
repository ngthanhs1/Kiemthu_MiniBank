package com.minibank.backend.account.dto;

public record AccountQrResponse(
	String accountNumber,
	String accountName,
	String payload
) {}
