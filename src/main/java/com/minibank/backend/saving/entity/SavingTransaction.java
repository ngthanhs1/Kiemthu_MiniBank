package com.minibank.backend.saving.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.minibank.backend.transaction.entity.Transaction;

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
@Table(name = "saving_transactions")
public class SavingTransaction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "saving_id", nullable = false)
	private Saving saving;

	@Column(name = "transaction_type", nullable = false, length = 50)
	private String transactionType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaction_id")
	private Transaction transaction;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal amount;

	@Column(name = "interest_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal interestAmount;

	@Column(name = "fee_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal feeAmount;

	@Column(columnDefinition = "text")
	private String description;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
