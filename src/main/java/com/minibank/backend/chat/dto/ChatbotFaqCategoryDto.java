package com.minibank.backend.chat.dto;

public record ChatbotFaqCategoryDto(
	Long id,
	String code,
	String name,
	String description,
	int faqCount
) {}
