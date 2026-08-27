package com.minibank.backend.ai.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AiCategorySummary(
	@JsonProperty("category") String category,
	@JsonProperty("amount") BigDecimal amount,
	@JsonProperty("percentage") BigDecimal percentage
) {}
