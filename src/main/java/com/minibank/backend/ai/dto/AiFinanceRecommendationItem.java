package com.minibank.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiFinanceRecommendationItem(
	@JsonProperty("type") String type,
	@JsonProperty("title") String title,
	@JsonProperty("message") String message,
	@JsonProperty("priority") String priority
) {}
