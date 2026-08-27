package com.minibank.backend.ai.dto;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiFinanceRecommendationRequest(
	@JsonProperty("user_id") long userId,
	@JsonProperty("month") String month,
	@JsonProperty("available_balance") BigDecimal availableBalance,
	@JsonProperty("income") BigDecimal income,
	@JsonProperty("expense") BigDecimal expense,
	@JsonProperty("category_summary") List<AiCategorySummary> categorySummary
) {}
