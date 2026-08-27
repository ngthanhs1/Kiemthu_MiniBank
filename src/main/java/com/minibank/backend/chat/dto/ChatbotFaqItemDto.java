package com.minibank.backend.chat.dto;

import java.util.List;

public record ChatbotFaqItemDto(
	Long id,
	Long categoryId,
	String categoryName,
	String categoryCode,
	Long parentFaqId,
	String question,
	String answer,
	long childCount,
	List<String> keywords,
	boolean active
) {}
