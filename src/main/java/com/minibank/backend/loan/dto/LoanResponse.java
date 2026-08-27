package com.minibank.backend.loan.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LoanResponse(
	Long id,
	Long loanApplicationId,
	Long userId,
	Long loanProductId,
	Long disbursementAccountId,
	Long repaymentAccountId,
	String code,
	BigDecimal approvedAmount,
	BigDecimal disbursedAmount,
	BigDecimal actualInterestRate,
	String interestCalculationMethod,
	BigDecimal outstandingPrincipal,
	BigDecimal outstandingInterest,
	BigDecimal overduePrincipal,
	BigDecimal overdueInterest,
	String status,
	String repaymentFrequency,
	int termMonths,
	Instant disbursedAt,
	Instant nextDueDate,
	Instant closedAt,
	Instant createdAt,
	List<RepaymentScheduleItem> schedule
) {
	public record RepaymentScheduleItem(
		Long id,
		Long loanId,
		int installmentNo,
		LocalDate dueDate,
		BigDecimal openingPrincipalBalance,
		BigDecimal principalDue,
		BigDecimal interestRate,
		BigDecimal interestDue,
		BigDecimal penaltyInterestDue,
		BigDecimal feeDue,
		BigDecimal totalDue,
		BigDecimal principalPaid,
		BigDecimal interestPaid,
		String status,
		Instant paidAt
	) {}
}
