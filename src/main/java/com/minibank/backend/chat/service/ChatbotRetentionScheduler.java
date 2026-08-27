package com.minibank.backend.chat.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.minibank.backend.chat.entity.ChatConversation;
import com.minibank.backend.chat.repository.ChatConversationRepository;
import com.minibank.backend.chat.repository.ChatMessageRepository;
import com.minibank.backend.chat.repository.ChatNoteRepository;

@Component
public class ChatbotRetentionScheduler {

	private final ChatConversationRepository conversationRepository;
	private final ChatMessageRepository messageRepository;
	private final ChatNoteRepository chatNoteRepository;
	private final long retentionDays;

	public ChatbotRetentionScheduler(
		ChatConversationRepository conversationRepository,
		ChatMessageRepository messageRepository,
		ChatNoteRepository chatNoteRepository,
		@Value("${app.chatbot.retention-days:7}") long retentionDays
	) {
		this.conversationRepository = conversationRepository;
		this.messageRepository = messageRepository;
		this.chatNoteRepository = chatNoteRepository;
		this.retentionDays = retentionDays;
	}

	@Scheduled(fixedDelayString = "${app.chatbot.retention-cleanup-delay-ms:3600000}")
	@Transactional
	public void cleanupExpiredChats() {
		Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
		List<ChatConversation> expiredConversations = conversationRepository.findByStartedAtBefore(cutoff);
		if (expiredConversations.isEmpty()) {
			return;
		}

		for (ChatConversation conversation : expiredConversations) {
			long conversationId = conversation.getId();
			chatNoteRepository.deleteByConversationId(conversationId);
			messageRepository.deleteByConversationId(conversationId);
		}

		conversationRepository.deleteAll(expiredConversations);
	}
}