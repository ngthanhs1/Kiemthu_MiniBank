package com.minibank.backend.chat.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.minibank.backend.chat.entity.ChatConversation;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {
	List<ChatConversation> findByUserIdOrderByStartedAtDesc(long userId);
	Optional<ChatConversation> findByIdAndUserId(long id, long userId);
	List<ChatConversation> findAllByOrderByStartedAtDesc();
	List<ChatConversation> findByStartedAtBefore(Instant startedAt);
}
