package com.minibank.backend.transaction.entity;

import java.math.BigDecimal;
import java.time.Instant;

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
@Table(name = "transaction_categories")
public class TransactionCategory {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "transaction_id", nullable = false)
	private Transaction transaction;

	@Column(name = "category_code", nullable = false, length = 50)
	private String categoryCode;

	@Column(name = "flow_type", nullable = false, length = 20)
	private String flowType;

	@Column(precision = 5, scale = 4)
	private BigDecimal confidence;

	@Column(nullable = false, length = 20)
	private String source;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tagged_by_user_id")
	private User taggedByUser;

	@CreationTimestamp
	@Column(name = "tagged_at", nullable = false, updatable = false)
	private Instant taggedAt;
}
