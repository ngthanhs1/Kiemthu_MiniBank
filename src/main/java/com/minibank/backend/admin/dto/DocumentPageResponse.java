package com.minibank.backend.admin.dto;

import java.util.List;

public record DocumentPageResponse(
	List<DocumentSummary> items,
	long total,
	int page,
	int size
) {}
