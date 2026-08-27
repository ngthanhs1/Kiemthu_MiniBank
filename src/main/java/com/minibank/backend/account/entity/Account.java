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
@Table(name = "accounts")
public class Account {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "account_number", nullable = false, unique = true, length = 32)
	private String accountNumber;

	@Column(name = "account_name", nullable = false)
	private String accountName;

	@Column(name = "account_type", nullable = false, length = 32)
	private String accountType;

	@Column(nullable = false, length = 10)
	private String currency;

	@Column(name = "available_balance", nullable = false, precision = 18, scale = 2)
	private BigDecimal availableBalance;

	@Column(name = "current_balance", nullable = false, precision = 18, scale = 2)
	private BigDecimal currentBalance;

	@Column(name = "daily_transfer_limit", nullable = false, precision = 18, scale = 2)
	private BigDecimal dailyTransferLimit;

	@Column(name = "daily_receive_limit", nullable = false, precision = 18, scale = 2)
	private BigDecimal dailyReceiveLimit;

	@Column(nullable = false, length = 32)
	private String status;

	@CreationTimestamp
	@Column(name = "opened_at", nullable = false, updatable = false)
	private Instant openedAt;

	@Column(name = "closed_at")
	private Instant closedAt;
}
