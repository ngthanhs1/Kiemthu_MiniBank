package com.minibank.backend.admin.dto;

public record SystemOverviewResponse(
	long totalUsers,
	long pendingUsers,
	long totalAccounts,
	long totalTransactions,
	long pendingTransactions
) {}
