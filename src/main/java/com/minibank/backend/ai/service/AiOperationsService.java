package com.minibank.backend.ai.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.repository.TransactionRepository;

@Service
public class AiOperationsService {
	private final TransactionRepository transactionRepository;
	private final AiTransactionClassificationService classificationService;
	private final AiRecommendationService recommendationService;
	private final AiSettingsService settingsService;

	public AiOperationsService(
		TransactionRepository transactionRepository,
		AiTransactionClassificationService classificationService,
		AiRecommendationService recommendationService,
		AiSettingsService settingsService
	) {
		this.transactionRepository = transactionRepository;
		this.classificationService = classificationService;
		this.recommendationService = recommendationService;
		this.settingsService = settingsService;
	}

	@Transactional
	public int runClassification() {
		Instant since = Instant.now().minusSeconds(60L * 60L * 24L * 30L);
		List<Transaction> transactions = transactionRepository.findCompletedSince(since);
		int processed = 0;
		for (Transaction transaction : transactions) {
			if (classificationService.classifyIfEligible(transaction)) {
				processed++;
			}
		}
		settingsService.markClassificationRun(Instant.now());
		return processed;
	}

	@Transactional
	public int runRecommendations() {
		int processed = recommendationService.generateDailyForAllUsers();
		settingsService.markRecommendationRun(Instant.now());
		return processed;
	}
}
