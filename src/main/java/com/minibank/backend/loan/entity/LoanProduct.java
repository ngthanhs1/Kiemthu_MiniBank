package com.minibank.backend.loan.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "loan_products")
public class LoanProduct {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 32)
	private String code;

	@Column(nullable = false)
	private String name;

	@Column(name = "loan_type", nullable = false, length = 32)
	private String loanType;

	@Column(nullable = false, length = 10)
	private String currency;

	@Column(name = "min_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal minAmount;

	@Column(name = "max_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal maxAmount;

	@Column(name = "min_term_months", nullable = false)
	private int minTermMonths;

	@Column(name = "max_term_months", nullable = false)
	private int maxTermMonths;

	@Column(name = "interest_rate_type", nullable = false, length = 16)
	private String interestRateType;

	@Column(name = "base_interest_rate", nullable = false, precision = 8, scale = 4)
	private BigDecimal baseInterestRate;

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

	@Column(nullable = false, length = 32)
	private String status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
