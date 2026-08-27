package com.minibank.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DocumentVerifyRequest(
	@NotBlank @Pattern(regexp = "approved|rejected", flags = Pattern.Flag.CASE_INSENSITIVE) String status,
	@Size(max = 2000) String note
) {}
