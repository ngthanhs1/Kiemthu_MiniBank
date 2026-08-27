package com.minibank.backend.transaction.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.entity.TransferQrIntent;
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
@Table(name = "transactions")
public class Transaction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "transaction_code", nullable = false, unique = true, length = 64)
	private String transactionCode;

	@Column(name = "idempotency_key", unique = true, length = 128)
	private String idempotencyKey;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "from_account_id")
	private Account fromAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "to_account_id")
	private Account toAccount;

	@Column(name = "transaction_type", nullable = false, length = 50)
	private String transactionType;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal amount;

	@Column(name = "fee_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal feeAmount;

	@Column(columnDefinition = "text")
	private String description;

	@Column(nullable = false, length = 32)
	private String status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "initiated_by_user_id")
	private User initiatedByUser;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "qr_transfer_intent_id")
	private TransferQrIntent qrTransferIntent;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;
}
