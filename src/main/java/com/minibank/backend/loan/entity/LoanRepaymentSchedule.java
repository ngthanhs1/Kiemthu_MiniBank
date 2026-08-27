package com.minibank.backend.loan.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "loan_repayment_schedule")
public class LoanRepaymentSchedule {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "loan_id", nullable = false)
	private Loan loan;

	@Column(name = "installment_no", nullable = false)
	private int installmentNo;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Column(name = "opening_principal_balance", nullable = false, precision = 18, scale = 2)
	private BigDecimal openingPrincipalBalance;

	@Column(name = "principal_due", nullable = false, precision = 18, scale = 2)
	private BigDecimal principalDue;

	@Column(name = "interest_rate", nullable = false, precision = 8, scale = 4)
	private BigDecimal interestRate;

	@Column(name = "interest_due", nullable = false, precision = 18, scale = 2)
	private BigDecimal interestDue;

	@Column(name = "penalty_interest_due", nullable = false, precision = 18, scale = 2)
	private BigDecimal penaltyInterestDue;

	@Column(name = "fee_due", nullable = false, precision = 18, scale = 2)
	private BigDecimal feeDue;

	@Column(name = "total_due", nullable = false, precision = 18, scale = 2)
	private BigDecimal totalDue;

	@Column(name = "principal_paid", nullable = false, precision = 18, scale = 2)
	private BigDecimal principalPaid;

	@Column(name = "interest_paid", nullable = false, precision = 18, scale = 2)
	private BigDecimal interestPaid;

	@Column(name = "penalty_interest_paid", nullable = false, precision = 18, scale = 2)
	private BigDecimal penaltyInterestPaid;

	@Column(name = "fee_paid", nullable = false, precision = 18, scale = 2)
	private BigDecimal feePaid;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(name = "paid_at")
	private Instant paidAt;
}
