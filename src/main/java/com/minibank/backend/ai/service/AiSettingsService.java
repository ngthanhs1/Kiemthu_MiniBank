package com.minibank.backend.ai.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minibank.backend.ai.entity.AiSettings;
import com.minibank.backend.ai.repository.AiSettingsRepository;
import com.minibank.backend.admin.dto.AdminAiSettingsRequest;
import com.minibank.backend.admin.dto.AdminAiSettingsResponse;

@Service
public class AiSettingsService {
	private final AiSettingsRepository aiSettingsRepository;

	public AiSettingsService(AiSettingsRepository aiSettingsRepository) {
		this.aiSettingsRepository = aiSettingsRepository;
	}

	@Transactional
	public AiSettings getOrCreate() {
		return aiSettingsRepository.findFirstByOrderByIdAsc()
			.orElseGet(() -> aiSettingsRepository.save(defaultSettings()));
	}

	@Transactional
	public AdminAiSettingsResponse update(AdminAiSettingsRequest request) {
		AiSettings settings = getOrCreate();
		if (request.classificationEnabled() != null) {
			settings.setClassificationEnabled(request.classificationEnabled());
		}
		if (request.classificationFrequencyMinutes() != null) {
			settings.setClassificationFrequencyMinutes(Math.max(1, request.classificationFrequencyMinutes()));
		}
		if (request.classificationStartTime() != null) {
			settings.setClassificationStartTime(normalizeTime(request.classificationStartTime()));
		}
		if (request.recommendationEnabled() != null) {
			settings.setRecommendationEnabled(request.recommendationEnabled());
		}
		if (request.recommendationFrequencyMinutes() != null) {
			settings.setRecommendationFrequencyMinutes(Math.max(1, request.recommendationFrequencyMinutes()));
		}
		if (request.recommendationStartTime() != null) {
			settings.setRecommendationStartTime(normalizeTime(request.recommendationStartTime()));
		}
		AiSettings saved = aiSettingsRepository.save(settings);
		return toResponse(saved);
	}

	@Transactional(readOnly = true)
	public AdminAiSettingsResponse getSettings() {
		return toResponse(getOrCreate());
	}

	@Transactional
	public void markClassificationRun(Instant time) {
		AiSettings settings = getOrCreate();
		settings.setLastClassificationRun(time);
		aiSettingsRepository.save(settings);
	}

	@Transactional
	public void markRecommendationRun(Instant time) {
		AiSettings settings = getOrCreate();
		settings.setLastRecommendationRun(time);
		aiSettingsRepository.save(settings);
	}

	private AdminAiSettingsResponse toResponse(AiSettings settings) {
		return new AdminAiSettingsResponse(
			settings.isClassificationEnabled(),
			settings.getClassificationFrequencyMinutes(),
			settings.getClassificationStartTime(),
			settings.getLastClassificationRun(),
			settings.isRecommendationEnabled(),
			settings.getRecommendationFrequencyMinutes(),
			settings.getRecommendationStartTime(),
			settings.getLastRecommendationRun()
		);
	}

	private AiSettings defaultSettings() {
		return AiSettings.builder()
			.classificationEnabled(true)
			.classificationFrequencyMinutes(60)
			.recommendationEnabled(true)
			.recommendationFrequencyMinutes(1440)
			.build();
	}

	private String normalizeTime(String value) {
		String trimmed = value == null ? null : value.trim();
		return trimmed == null || trimmed.isBlank() ? null : trimmed;
	}
}
