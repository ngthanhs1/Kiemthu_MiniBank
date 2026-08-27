package com.minibank.backend.admin.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminFaqItemRequest(
	@NotNull Long categoryId,
	Long parentFaqId,
	@NotBlank @Size(max = 255) String question,
	@NotBlank @Size(max = 6000) String answer,
	@NotNull Boolean active,
	List<@Size(max = 120) String> keywords
) {}
