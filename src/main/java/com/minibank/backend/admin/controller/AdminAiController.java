package com.minibank.backend.admin.controller;

import java.time.Instant;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.admin.dto.AdminAiRunResponse;
import com.minibank.backend.admin.dto.AdminAiSettingsRequest;
import com.minibank.backend.admin.dto.AdminAiSettingsResponse;
import com.minibank.backend.ai.service.AiOperationsService;
import com.minibank.backend.ai.service.AiSettingsService;

@RestController
@RequestMapping("/api/admin/ai")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAiController {
	private final AiSettingsService settingsService;
	private final AiOperationsService operationsService;

	public AdminAiController(
		AiSettingsService settingsService,
		AiOperationsService operationsService
	) {
		this.settingsService = settingsService;
		this.operationsService = operationsService;
	}

	@GetMapping("/settings")
	public AdminAiSettingsResponse settings() {
		return settingsService.getSettings();
	}

	@PutMapping("/settings")
	public AdminAiSettingsResponse update(@RequestBody AdminAiSettingsRequest request) {
		return settingsService.update(request);
	}

	@PostMapping("/run-classification")
	public AdminAiRunResponse runClassification() {
		int processed = operationsService.runClassification();
		return new AdminAiRunResponse("ok", processed, Instant.now());
	}

	@PostMapping("/run-recommendations")
	public AdminAiRunResponse runRecommendations() {
		int processed = operationsService.runRecommendations();
		return new AdminAiRunResponse("ok", processed, Instant.now());
	}

	@PostMapping("/train")
	public AdminAiRunResponse train() {
		return new AdminAiRunResponse("queued", 0, Instant.now());
	}
}
