package com.minibank.backend.chat.dto;

import java.util.List;

public record MobileChatbotBootstrapResponse(
	List<ChatbotFaqCategoryDto> categories,
	List<ChatbotFaqItemDto> suggestedQuestions
) {}
