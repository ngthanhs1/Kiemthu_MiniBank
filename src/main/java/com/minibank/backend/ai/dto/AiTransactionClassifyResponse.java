package com.minibank.backend.ai.dto;

import java.math.BigDecimal;
import java.util.List;

public record AiTransactionClassifyResponse(
	long transactionId,
	String direction,
	String category,
	String categoryName,
	BigDecimal confidence,
	List<String> matchedKeywords,
	String source
) {}
