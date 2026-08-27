package com.minibank.backend.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiFinanceRecommendationResponse(
	@JsonProperty("user_id") long userId,
	@JsonProperty("month") String month,
	@JsonProperty("risk_level") String riskLevel,
	@JsonProperty("saving_score") int savingScore,
	@JsonProperty("recommendations") List<AiFinanceRecommendationItem> recommendations,
	@JsonProperty("source") String source
) {}
