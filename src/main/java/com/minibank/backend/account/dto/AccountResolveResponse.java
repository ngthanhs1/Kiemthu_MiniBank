package com.minibank.backend.account.dto;

public record AccountResolveResponse(
	Long id,
	String accountNumber,
	String accountName
) {}
