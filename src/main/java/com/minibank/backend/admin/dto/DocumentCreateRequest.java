package com.minibank.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DocumentCreateRequest(
	@NotBlank String ownerType,
	@NotNull Long ownerId,
	@NotBlank String documentType,
	@NotBlank String fileUrl,
	@Size(max = 255) String fileName,
	@Size(max = 100) String mimeType,
	@Size(max = 2000) String note
) {}
