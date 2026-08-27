package com.minibank.backend.admin.dto;

public record AdminFaqCategoryResponse(
	Long id,
	String code,
	String name,
	String description,
	Integer sortOrder,
	boolean active,
	long faqCount
) {}
