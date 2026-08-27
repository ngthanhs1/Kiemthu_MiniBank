package com.minibank.backend.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AdminDashboardResponse(
	Instant updatedAt,
	List<Metric> metrics,
	List<TimePoint> transactionCountSeries,
	List<TimePoint> transactionAmountSeries,
	List<ProductMetric> productPerformance,
	List<RecentTransaction> recentTransactions,
	List<RequestMetric> pendingRequests
) {
	public record Metric(
		String key,
		String label,
		BigDecimal value,
		String valueType,
		String delta,
		String tone,
		String href
	) {}

	public record TimePoint(
		String label,
		BigDecimal value
	) {}

	public record ProductMetric(
		String label,
		BigDecimal value
	) {}

	public record RecentTransaction(
		Long id,
		String transactionCode,
		String accountName,
		String description,
		BigDecimal amount,
		String direction,
		String transactionType,
		String status,
		Instant createdAt
	) {}

	public record RequestMetric(
		String label,
		long value,
		String href
	) {}
}
