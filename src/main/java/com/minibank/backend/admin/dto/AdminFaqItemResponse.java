package com.minibank.backend.admin.dto;

import java.util.List;

public record AdminFaqItemResponse(
	Long id,
	Long categoryId,
	String categoryName,
	String categoryCode,
	Long parentFaqId,
	String question,
	String answer,
	boolean active,
	long childCount,
	List<String> keywords
) {}
