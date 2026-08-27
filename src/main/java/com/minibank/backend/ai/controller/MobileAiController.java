package com.minibank.backend.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.ai.dto.AiFinanceRecommendationResponse;
import com.minibank.backend.ai.service.AiRecommendationService;
import com.minibank.backend.common.security.CurrentJwt;

@RestController
@RequestMapping("/api/mobile/ai")
public class MobileAiController {
	private final AiRecommendationService recommendationService;

	public MobileAiController(AiRecommendationService recommendationService) {
		this.recommendationService = recommendationService;
	}

	@GetMapping("/recommendations/daily")
	public AiFinanceRecommendationResponse dailyRecommendations() {
		return recommendationService.getDailyRecommendation(CurrentJwt.requireUserId());
	}
}
