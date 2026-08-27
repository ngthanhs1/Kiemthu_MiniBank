package com.minibank.backend.user.dto;

import java.time.LocalDate;
import java.util.List;

public record ProfileResponse(
	Long id,
	String phone,
	String email,
	String fullName,
	LocalDate dob,
	String address,
	String status,
	String customerRank,
	boolean hasTransactionPin,
	boolean hasPublicKey,
	String deviceId,
	List<AccountSummary> accounts
) {
	public record AccountSummary(
		Long id,
		String accountNumber,
		String accountName,
		String status
	) {}
}
