package com.minibank.backend.support.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServiceRequestRequest(
	@NotNull(message = "Request type is required")
	String requestType,

	@NotBlank(message = "Title is required")
	String title,

	String description,

	String payloadJson,

	String priorityTag
) {}
