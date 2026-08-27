package com.minibank.backend.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minibank.backend.chat.entity.ChatNote;

@Repository
public interface ChatNoteRepository extends JpaRepository<ChatNote, Long> {
	List<ChatNote> findByConversationIdOrderByCreatedAtDesc(long conversationId);
	void deleteByConversationId(long conversationId);
}
