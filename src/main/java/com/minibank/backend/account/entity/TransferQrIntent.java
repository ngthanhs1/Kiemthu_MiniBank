package com.minibank.backend.account.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.minibank.backend.user.entity.User;

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
@Table(name = "qr_transfer_intents")
public class TransferQrIntent {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "intent_token", nullable = false, unique = true, length = 64)
	private String intentToken;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Column(name = "amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal amount;

	@Column(name = "status", nullable = false, length = 32)
	private String status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by_user_id")
	private User createdByUser;

	@Column(name = "claimed_by_user_id")
	private Long claimedByUserId;

	@Column(name = "completed_transaction_id")
	private Long completedTransactionId;

	@Column(name = "payload", nullable = false, columnDefinition = "text")
	private String payload;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(name = "claimed_at")
	private Instant claimedAt;

	@Column(name = "completed_at")
	private Instant completedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}