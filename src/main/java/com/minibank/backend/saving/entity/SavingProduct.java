package com.minibank.backend.saving.entity;

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
@Table(name = "saving_products")
public class SavingProduct {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 32)
	private String code;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, length = 10)
	private String currency;

	@Column(name = "term_unit", nullable = false, length = 16)
	private String termUnit;

	@Column(name = "term_value", nullable = false)
	private int termValue;

	@Column(name = "interest_rate_type", nullable = false, length = 16)
	private String interestRateType;

	@Column(name = "base_interest_rate", nullable = false, precision = 8, scale = 4)
	private BigDecimal baseInterestRate;

	@Column(name = "penalty_interest_rate", precision = 8, scale = 4)
	private BigDecimal penaltyInterestRate;

	@Column(name = "bonus_interest_rate", precision = 8, scale = 4)
	private BigDecimal bonusInterestRate;

	@Column(name = "interest_accrual_frequency", nullable = false, length = 32)
	private String interestAccrualFrequency;

	@Column(name = "interest_posting_frequency", nullable = false, length = 32)
	private String interestPostingFrequency;

	@Column(nullable = false)
	private boolean capitalized;

	@Column(name = "min_open_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal minOpenAmount;

	@Column(name = "max_open_amount", precision = 18, scale = 2)
	private BigDecimal maxOpenAmount;

	@Column(name = "deposit_fee_rate", precision = 14, scale = 4)
	private BigDecimal depositFeeRate;

	@Column(name = "deposit_fee_flat", precision = 18, scale = 2)
	private BigDecimal depositFeeFlat;

	@Column(name = "withdrawal_fee_rate", precision = 14, scale = 4)
	private BigDecimal withdrawalFeeRate;

	@Column(name = "withdrawal_fee_flat", precision = 18, scale = 2)
	private BigDecimal withdrawalFeeFlat;

	@Column(name = "close_fee_rate", precision = 14, scale = 4)
	private BigDecimal closeFeeRate;

	@Column(name = "close_fee_flat", precision = 18, scale = 2)
	private BigDecimal closeFeeFlat;

	@Column(name = "management_fee_rate", precision = 14, scale = 4)
	private BigDecimal managementFeeRate;

	@Column(name = "management_fee_flat", precision = 18, scale = 2)
	private BigDecimal managementFeeFlat;

	@Column(name = "management_fee_frequency", length = 32)
	private String managementFeeFrequency;

	@Column(nullable = false, length = 32)
	private String status;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
