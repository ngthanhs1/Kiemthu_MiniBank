package com.minibank.backend.admin.dto;

import java.time.Instant;

public record AdminAiSettingsResponse(
	boolean classificationEnabled,
	int classificationFrequencyMinutes,
	String classificationStartTime,
	Instant lastClassificationRun,
	boolean recommendationEnabled,
	int recommendationFrequencyMinutes,
	String recommendationStartTime,
	Instant lastRecommendationRun
) {}
