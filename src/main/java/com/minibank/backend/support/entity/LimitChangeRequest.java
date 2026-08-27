package com.minibank.backend.support.entity;

import java.math.BigDecimal;

import com.minibank.backend.account.entity.Account;

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
@Table(name = "limit_change_requests")
public class LimitChangeRequest {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "service_request_id", nullable = false)
	private ServiceRequest serviceRequest;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Column(name = "current_daily_transfer_limit", nullable = false, precision = 18, scale = 2)
	private BigDecimal currentDailyTransferLimit;

	@Column(name = "requested_daily_transfer_limit", nullable = false, precision = 18, scale = 2)
	private BigDecimal requestedDailyTransferLimit;

	@Column(columnDefinition = "text")
	private String reason;
}
