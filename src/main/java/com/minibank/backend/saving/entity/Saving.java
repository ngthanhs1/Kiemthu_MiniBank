package com.minibank.backend.saving.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.admin.entity.AdminUser;
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
@Table(name = "savings")
public class Saving {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 32)
	private String code;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "saving_product_id", nullable = false)
	private SavingProduct savingProduct;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "source_account_id", nullable = false)
	private Account sourceAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "settlement_account_id")
	private Account settlementAccount;

	@Column(name = "principal_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal principalAmount;

	@Column(name = "actual_interest_rate", nullable = false, precision = 8, scale = 4)
	private BigDecimal actualInterestRate;

	@Column(name = "interest_rate_type", nullable = false, length = 16)
	private String interestRateType;

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

	@Column(name = "accrued_interest_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal accruedInterestAmount;

	@Column(name = "posted_interest_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal postedInterestAmount;

	@Column(name = "projected_maturity_amount", precision = 18, scale = 2)
	private BigDecimal projectedMaturityAmount;

	@Column(name = "deposit_amount_min", precision = 18, scale = 2)
	private BigDecimal depositAmountMin;

	@Column(name = "deposit_amount_max", precision = 18, scale = 2)
	private BigDecimal depositAmountMax;

	@Column(name = "deposit_fee_rate", precision = 14, scale = 4)
	private BigDecimal depositFeeRate;

	@Column(name = "deposit_fee_flat", precision = 18, scale = 2)
	private BigDecimal depositFeeFlat;

	@Column(name = "withdrawal_amount_min", precision = 18, scale = 2)
	private BigDecimal withdrawalAmountMin;

	@Column(name = "withdrawal_amount_max", precision = 18, scale = 2)
	private BigDecimal withdrawalAmountMax;

	@Column(name = "withdrawal_fee_rate", precision = 14, scale = 4)
	private BigDecimal withdrawalFeeRate;

	@Column(name = "withdrawal_fee_flat", precision = 18, scale = 2)
	private BigDecimal withdrawalFeeFlat;

	@Column(name = "entry_fee_rate", precision = 14, scale = 4)
	private BigDecimal entryFeeRate;

	@Column(name = "entry_fee_flat", precision = 18, scale = 2)
	private BigDecimal entryFeeFlat;

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

	@Column(name = "term_unit", nullable = false, length = 16)
	private String termUnit;

	@Column(name = "term_value", nullable = false)
	private int termValue;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(name = "rejection_reason", columnDefinition = "text")
	private String rejectionReason;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by_id")
	private AdminUser reviewedBy;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@Column(name = "open_date")
	private Instant openDate;

	@Column(name = "maturity_date")
	private Instant maturityDate;

	@Column(name = "close_date")
	private Instant closeDate;

	@Column(name = "auto_renew", nullable = false)
	private boolean autoRenew;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_id", nullable = false)
	private AdminUser createdBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "opened_by_id")
	private AdminUser openedBy;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "closed_by_id")
	private AdminUser closedBy;

	@Column(name = "agreement_accepted_at")
	private Instant agreementAcceptedAt;

	@Column(name = "agreement_version", length = 32)
	private String agreementVersion;

	@Column(nullable = false)
	private boolean locked;
}
