package com.minibank.backend.chat.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.chat.dto.ChatbotFaqItemDto;
import com.minibank.backend.chat.dto.MobileChatConversationDetailResponse;
import com.minibank.backend.chat.dto.MobileChatConversationSummary;
import com.minibank.backend.chat.dto.MobileChatEscalateRequest;
import com.minibank.backend.chat.dto.MobileChatbotBootstrapResponse;
import com.minibank.backend.chat.dto.MobileChatbotSendRequest;
import com.minibank.backend.chat.dto.MobileChatbotSendResponse;
import com.minibank.backend.chat.service.GuidedChatbotService;
import com.minibank.backend.common.security.CurrentJwt;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/chatbot")
public class MobileChatbotController {
	private final GuidedChatbotService chatbotService;

	public MobileChatbotController(GuidedChatbotService chatbotService) {
		this.chatbotService = chatbotService;
	}

	@GetMapping("/bootstrap")
	public MobileChatbotBootstrapResponse bootstrap() {
		return chatbotService.bootstrap();
	}

	@GetMapping("/faq/items")
	public List<ChatbotFaqItemDto> faqItems(
		@RequestParam(value = "categoryId", required = false) Long categoryId,
		@RequestParam(value = "parentFaqId", required = false) Long parentFaqId
	) {
		return chatbotService.listFaqItems(categoryId, parentFaqId);
	}

	@GetMapping("/conversations")
	public List<MobileChatConversationSummary> conversations() {
		return chatbotService.listConversations(CurrentJwt.requireUserId());
	}

	@GetMapping("/conversations/{conversationId}")
	public MobileChatConversationDetailResponse conversation(@PathVariable long conversationId) {
		return chatbotService.getConversation(CurrentJwt.requireUserId(), conversationId);
	}

	@PostMapping("/messages")
	public MobileChatbotSendResponse send(@Valid @RequestBody MobileChatbotSendRequest request) {
		return chatbotService.send(CurrentJwt.requireUserId(), request);
	}

	@PostMapping("/conversations/{conversationId}/escalate")
	public MobileChatConversationDetailResponse escalate(@PathVariable long conversationId) {
		return chatbotService.escalate(CurrentJwt.requireUserId(), conversationId);
	}

	@PostMapping("/conversations/escalate")
	public MobileChatConversationDetailResponse escalateTemporary(@RequestBody MobileChatEscalateRequest request) {
		return chatbotService.escalateTemporary(CurrentJwt.requireUserId(), request);
	}
}
