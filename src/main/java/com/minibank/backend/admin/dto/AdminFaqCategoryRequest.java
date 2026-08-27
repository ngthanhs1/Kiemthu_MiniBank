package com.minibank.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminFaqCategoryRequest(
	@NotBlank @Size(max = 64) String code,
	@NotBlank @Size(max = 120) String name,
	@Size(max = 2000) String description,
	@NotNull Integer sortOrder,
	@NotNull Boolean active
) {}
