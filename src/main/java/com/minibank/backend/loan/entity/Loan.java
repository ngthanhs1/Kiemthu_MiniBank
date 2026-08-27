package com.minibank.backend.loan.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.minibank.backend.account.entity.Account;
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
@Table(name = "loans")
public class Loan {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 32)
	private String code;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "loan_application_id", nullable = false)
	private LoanApplication loanApplication;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "loan_product_id")
	private LoanProduct loanProduct;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "disbursement_account_id")
	private Account disbursementAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "repayment_account_id")
	private Account repaymentAccount;

	@Column(name = "approved_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal approvedAmount;

	@Column(name = "disbursed_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal disbursedAmount;

	@Column(name = "interest_rate_type", nullable = false, length = 16)
	private String interestRateType;

	@Column(name = "actual_interest_rate", nullable = false, precision = 8, scale = 4)
	private BigDecimal actualInterestRate;

	@Column(name = "penalty_interest_rate", precision = 8, scale = 4)
	private BigDecimal penaltyInterestRate;

	@Column(name = "grace_interest_rate", precision = 8, scale = 4)
	private BigDecimal graceInterestRate;

	@Column(name = "processing_fee_rate", precision = 14, scale = 4)
	private BigDecimal processingFeeRate;

	@Column(name = "processing_fee_flat", precision = 18, scale = 2)
	private BigDecimal processingFeeFlat;

	@Column(name = "early_repayment_fee_rate", precision = 14, scale = 4)
	private BigDecimal earlyRepaymentFeeRate;

	@Column(name = "early_repayment_fee_flat", precision = 18, scale = 2)
	private BigDecimal earlyRepaymentFeeFlat;

	@Column(name = "interest_calculation_method", nullable = false, length = 32)
	private String interestCalculationMethod;

	@Column(name = "repayment_frequency", nullable = false, length = 32)
	private String repaymentFrequency;

	@Column(name = "term_months", nullable = false)
	private int termMonths;

	@Column(name = "outstanding_principal", nullable = false, precision = 18, scale = 2)
	private BigDecimal outstandingPrincipal;

	@Column(name = "outstanding_interest", nullable = false, precision = 18, scale = 2)
	private BigDecimal outstandingInterest;

	@Column(name = "overdue_principal", nullable = false, precision = 18, scale = 2)
	private BigDecimal overduePrincipal;

	@Column(name = "overdue_interest", nullable = false, precision = 18, scale = 2)
	private BigDecimal overdueInterest;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(name = "disbursed_at")
	private Instant disbursedAt;

	@Column(name = "next_due_date")
	private Instant nextDueDate;

	@Column(name = "closed_at")
	private Instant closedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
