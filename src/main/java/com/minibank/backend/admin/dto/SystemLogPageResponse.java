package com.minibank.backend.admin.dto;

import java.util.List;

public record SystemLogPageResponse(
	List<SystemLogItem> items,
	long total,
	int page,
	int size
) {}
