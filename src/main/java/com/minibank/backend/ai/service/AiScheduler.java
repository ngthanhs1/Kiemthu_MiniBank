package com.minibank.backend.ai.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.minibank.backend.ai.entity.AiSettings;

@Component
public class AiScheduler {
	private final AiSettingsService settingsService;
	private final AiOperationsService operationsService;

	public AiScheduler(AiSettingsService settingsService, AiOperationsService operationsService) {
		this.settingsService = settingsService;
		this.operationsService = operationsService;
	}

	@Scheduled(fixedDelayString = "60000")
	public void tick() {
		AiSettings settings = settingsService.getOrCreate();
		Instant now = Instant.now();

		if (settings.isClassificationEnabled() && shouldRun(now, settings.getLastClassificationRun(), settings.getClassificationFrequencyMinutes(), settings.getClassificationStartTime())) {
			operationsService.runClassification();
		}

		if (settings.isRecommendationEnabled() && shouldRun(now, settings.getLastRecommendationRun(), settings.getRecommendationFrequencyMinutes(), settings.getRecommendationStartTime())) {
			operationsService.runRecommendations();
		}
	}

	private boolean shouldRun(Instant now, Instant lastRun, int frequencyMinutes, String startTime) {
		Instant startBoundary = resolveStartBoundary(startTime);
		if (startBoundary != null && now.isBefore(startBoundary)) {
			return false;
		}
		if (lastRun == null) {
			return true;
		}
		long elapsed = now.getEpochSecond() - lastRun.getEpochSecond();
		return elapsed >= frequencyMinutes * 60L;
	}

	private Instant resolveStartBoundary(String startTime) {
		if (startTime == null || startTime.isBlank()) {
			return null;
		}
		try {
			LocalTime time = LocalTime.parse(startTime.trim());
			return LocalDate.now(ZoneOffset.UTC).atTime(time).toInstant(ZoneOffset.UTC);
		} catch (Exception ex) {
			return null;
		}
	}
}
