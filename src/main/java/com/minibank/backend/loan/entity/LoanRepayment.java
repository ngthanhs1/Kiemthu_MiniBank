package com.minibank.backend.loan.entity;

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
@Table(name = "loan_repayments")
public class LoanRepayment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "loan_id", nullable = false)
	private Loan loan;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "repayment_schedule_id")
	private LoanRepaymentSchedule repaymentSchedule;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transaction_id")
	private Transaction transaction;

	@Column(name = "paid_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal paidAmount;

	@Column(name = "principal_component", nullable = false, precision = 18, scale = 2)
	private BigDecimal principalComponent;

	@Column(name = "interest_component", nullable = false, precision = 18, scale = 2)
	private BigDecimal interestComponent;

	@Column(name = "penalty_interest_component", nullable = false, precision = 18, scale = 2)
	private BigDecimal penaltyInterestComponent;

	@Column(name = "fee_component", nullable = false, precision = 18, scale = 2)
	private BigDecimal feeComponent;

	@CreationTimestamp
	@Column(name = "paid_at", nullable = false, updatable = false)
	private Instant paidAt;
}
