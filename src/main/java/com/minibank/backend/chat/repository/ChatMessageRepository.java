package com.minibank.backend.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minibank.backend.chat.entity.ChatMessage;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
	List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(long conversationId);
	Optional<ChatMessage> findTop1ByConversationIdOrderByCreatedAtDesc(long conversationId);
	void deleteByConversationId(long conversationId);
}
