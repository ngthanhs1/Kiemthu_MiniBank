package com.minibank.backend.account.dto;

import java.util.List;

public record AccountSuggestionsResponse(
	String desired,
	List<String> suggestions
) {}
