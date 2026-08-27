package com.minibank.backend.admin.dto;

public record AdminAiSettingsRequest(
	Boolean classificationEnabled,
	Integer classificationFrequencyMinutes,
	String classificationStartTime,
	Boolean recommendationEnabled,
	Integer recommendationFrequencyMinutes,
	String recommendationStartTime
) {}
