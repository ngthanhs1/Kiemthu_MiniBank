package com.minibank.backend.chat.dto;

import java.util.List;

public record MobileChatbotSendResponse(
	Long conversationId,
	MobileChatMessageDto userMessage,
	MobileChatMessageDto botMessage,
	Long matchedFaqId,
	String matchedCategoryCode,
	Integer confidence,
	List<ChatbotFaqItemDto> followUps,
	boolean escalated
) {}
