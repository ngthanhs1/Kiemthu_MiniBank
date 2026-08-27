package com.minibank.backend.loan.entity;

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
@Table(name = "loan_applications")
public class LoanApplication {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "loan_product_id")
	private LoanProduct loanProduct;

	@Column(name = "requested_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal requestedAmount;

	@Column(name = "requested_term_months", nullable = false)
	private int requestedTermMonths;

	@Column(name = "monthly_income", precision = 18, scale = 2)
	private BigDecimal monthlyIncome;

	@Column(columnDefinition = "text")
	private String purpose;

	@Column(name = "collateral_description", columnDefinition = "text")
	private String collateralDescription;

	@Column(name = "income_proof_url", columnDefinition = "text")
	private String incomeProofUrl;

	@Column(name = "collateral_proof_url", columnDefinition = "text")
	private String collateralProofUrl;

	@Column(name = "priority_tag", length = 32)
	private String priorityTag;

	@Column(nullable = false, length = 32)
	private String status;

	@CreationTimestamp
	@Column(name = "submitted_at", nullable = false, updatable = false)
	private Instant submittedAt;

	@Column(name = "reviewed_at")
	private Instant reviewedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "disbursement_account_id")
	private Account disbursementAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "repayment_account_id")
	private Account repaymentAccount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "reviewed_by_id")
	private AdminUser reviewedBy;

	@Column(name = "review_note", columnDefinition = "text")
	private String reviewNote;
}
