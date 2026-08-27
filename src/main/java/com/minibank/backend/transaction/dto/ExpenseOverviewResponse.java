package com.minibank.backend.transaction.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseOverviewResponse(
	String flowType,
	BigDecimal totalIncome,
	BigDecimal totalExpense,
	BigDecimal selectedFlowTotal,
	long selectedFlowTransactionCount,
	long unclassifiedTransactionCount,
	List<ExpenseCategorySummaryResponse> categories,
	List<ExpenseUnclassifiedTransactionResponse> unclassifiedTransactions,
	List<ExpenseMonthlyTrendResponse> monthlyTrend
) {}
