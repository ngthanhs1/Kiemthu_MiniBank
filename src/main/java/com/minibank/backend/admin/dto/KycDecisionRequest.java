package com.minibank.backend.admin.dto;

import jakarta.validation.constraints.Size;

public record KycDecisionRequest(
	@Size(max = 2000) String note
) {}
