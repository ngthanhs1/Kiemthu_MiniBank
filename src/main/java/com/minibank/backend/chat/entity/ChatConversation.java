package com.minibank.backend.chat.entity;

import java.time.Instant;

import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.user.entity.User;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "chat_conversations")
public class ChatConversation {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 32)
	private String channel;

	@Column(nullable = false, length = 32)
	private String status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_admin_user_id")
	private AdminUser assignedAdminUser;

	@CreationTimestamp
	@Column(name = "started_at", nullable = false, updatable = false)
	private Instant startedAt;

	@Column(name = "ended_at")
	private Instant endedAt;

	@Column(name = "last_intent", length = 120)
	private String lastIntent;

	@Column(name = "last_confidence")
	private Integer lastConfidence;

	@Column(name = "escalated_at")
	private Instant escalatedAt;
}
