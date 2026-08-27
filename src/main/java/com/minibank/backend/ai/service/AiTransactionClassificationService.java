package com.minibank.backend.ai.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.minibank.backend.ai.dto.AiTransactionClassifyResponse;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.entity.TransactionCategory;
import com.minibank.backend.transaction.repository.TransactionCategoryRepository;

@Service
public class AiTransactionClassificationService {
	private final AiClient aiClient;
	private final TransactionCategoryRepository transactionCategoryRepository;
	private final AiSettingsService settingsService;

	public AiTransactionClassificationService(
		AiClient aiClient,
		TransactionCategoryRepository transactionCategoryRepository,
		AiSettingsService settingsService
	) {
		this.aiClient = aiClient;
		this.transactionCategoryRepository = transactionCategoryRepository;
		this.settingsService = settingsService;
	}

	public boolean classifyIfEligible(Transaction tx) {
		if (!settingsService.getOrCreate().isClassificationEnabled()) {
			return false;
		}
		Optional<TransactionCategory> latest = transactionCategoryRepository
			.findFirstByTransactionIdOrderByTaggedAtDescIdDesc(tx.getId());
		if (latest.isPresent()) {
			return false;
		}

		return aiClient.classifyTransaction(tx)
			.flatMap(result -> mapCategory(result, tx))
			.map(mapped -> {
				transactionCategoryRepository.deleteByTransactionId(tx.getId());
				transactionCategoryRepository.save(
					TransactionCategory.builder()
						.transaction(tx)
						.categoryCode(mapped.categoryCode)
						.flowType(mapped.flowType)
						.confidence(mapped.confidence)
						.source("ai_rule")
						.taggedByUser(null)
						.build()
				);
				return true;
			})
			.orElse(false);
	}

	private Optional<MappedCategory> mapCategory(AiTransactionClassifyResponse response, Transaction tx) {
		String direction = response.direction() == null ? "OUT" : response.direction().toUpperCase(Locale.US);
		String flowType = "IN".equals(direction) ? "in" : "out";
		String category = response.category() == null ? "OTHER" : response.category().toUpperCase(Locale.US);

		String categoryCode;
		if ("in".equals(flowType)) {
			categoryCode = switch (category) {
				case "SALARY" -> "luong";
				case "TRANSFER", "SAVING" -> "chuyen_khoan";
				default -> "thu_nhap_khac";
			};
		} else {
			categoryCode = switch (category) {
				case "FOOD" -> "an_uong";
				case "SHOPPING" -> "mua_sam";
				case "TRANSPORT" -> "di_chuyen";
				case "BILL" -> "tien_ich";
				default -> "khac";
			};
		}

		BigDecimal confidence = response.confidence() == null ? BigDecimal.valueOf(0.3) : response.confidence();
		return Optional.of(new MappedCategory(categoryCode, flowType, confidence));
	}

	private record MappedCategory(String categoryCode, String flowType, BigDecimal confidence) {}
}
