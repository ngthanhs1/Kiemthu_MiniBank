package com.minibank.backend.chat.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.minibank.backend.admin.dto.AdminChatMessageResponse;
import com.minibank.backend.chat.dto.ChatConversationEvent;
import com.minibank.backend.chat.entity.ChatConversation;
import com.minibank.backend.chat.entity.ChatMessage;

@Service
public class ChatRealtimeService {
	private final SimpMessagingTemplate messagingTemplate;

	public ChatRealtimeService(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	public void broadcastConversation(ChatConversation conversation, String lastMessagePreview) {
		ChatConversationEvent event = new ChatConversationEvent(
			conversation.getId(),
			conversation.getUser().getId(),
			conversation.getUser().getFullName(),
			conversation.getUser().getPhone(),
			conversation.getUser().getCustomerRank(),
			conversation.getStatus(),
			conversation.getStartedAt(),
			conversation.getEscalatedAt(),
			conversation.getAssignedAdminUser() == null ? null : conversation.getAssignedAdminUser().getId(),
			conversation.getAssignedAdminUser() == null ? null : conversation.getAssignedAdminUser().getUsername(),
			lastMessagePreview
		);
		messagingTemplate.convertAndSend("/topic/chat-waiting", event);
		messagingTemplate.convertAndSend("/topic/chat/" + conversation.getId() + "/status", event);
	}

	public void broadcastMessage(ChatMessage message) {
		messagingTemplate.convertAndSend(
			"/topic/chat/" + message.getConversation().getId(),
			new AdminChatMessageResponse(
				message.getId(),
				message.getSenderType(),
				message.getSenderId(),
				message.getMessageType(),
				message.getContent(),
				message.getCreatedAt()
			)
		);
	}

	public void broadcastTyping(long conversationId, Object payload) {
		messagingTemplate.convertAndSend("/topic/chat/" + conversationId + "/typing", payload);
	}
}
