package com.minibank.backend.chat.controller;

import java.util.Locale;
import java.util.Map;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import com.minibank.backend.chat.dto.ChatSendRequest;
import com.minibank.backend.chat.dto.ChatTypingRequest;
import com.minibank.backend.chat.entity.ChatConversation;
import com.minibank.backend.chat.entity.ChatMessage;
import com.minibank.backend.chat.repository.ChatConversationRepository;
import com.minibank.backend.chat.repository.ChatMessageRepository;
import com.minibank.backend.chat.service.ChatRealtimeService;

@Controller
public class ChatWebSocketController {
	private final ChatConversationRepository conversationRepository;
	private final ChatMessageRepository messageRepository;
	private final ChatRealtimeService realtimeService;

	public ChatWebSocketController(
		ChatConversationRepository conversationRepository,
		ChatMessageRepository messageRepository,
		ChatRealtimeService realtimeService
	) {
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.realtimeService = realtimeService;
	}

	@Transactional
	@MessageMapping("/chat.send")
	public void send(ChatSendRequest request) {
		ChatConversation conversation = conversationRepository.findById(request.conversationId())
			.orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
		String senderType = normalizeSenderType(request.senderType());
		ChatMessage saved = messageRepository.save(ChatMessage.builder()
			.conversation(conversation)
			.senderType(senderType)
			.senderId(request.senderId())
			.messageType("TEXT")
			.content(request.content().trim())
			.build());

		if ("ADMIN".equals(senderType)) {
			conversation.setStatus("IN_PROGRESS");
			conversationRepository.save(conversation);
		}

		realtimeService.broadcastMessage(saved);
		realtimeService.broadcastConversation(conversation, saved.getContent());
	}

	@MessageMapping("/chat.typing")
	public void typing(ChatTypingRequest request) {
		realtimeService.broadcastTyping(request.conversationId(), Map.of(
			"conversationId", request.conversationId(),
			"senderType", normalizeSenderType(request.senderType()),
			"senderId", request.senderId() == null ? 0 : request.senderId(),
			"typing", request.typing()
		));
	}

	private String normalizeSenderType(String senderType) {
		if (senderType == null) return "USER";
		String normalized = senderType.trim().toUpperCase(Locale.ROOT);
		return switch (normalized) {
			case "USER", "CUSTOMER", "ADMIN", "BOT", "SYSTEM" -> "CUSTOMER".equals(normalized) ? "USER" : normalized;
			default -> "USER";
		};
	}
}
