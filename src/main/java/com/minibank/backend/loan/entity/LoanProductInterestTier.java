package com.minibank.backend.loan.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "loan_product_interest_tiers")
public class LoanProductInterestTier {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "loan_product_id", nullable = false)
	private LoanProduct loanProduct;

	@Column(name = "min_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal minAmount;

	@Column(name = "max_amount", precision = 18, scale = 2)
	private BigDecimal maxAmount;

	@Column(name = "min_term_months")
	private Integer minTermMonths;

	@Column(name = "max_term_months")
	private Integer maxTermMonths;

	@Column(name = "interest_rate", nullable = false, precision = 8, scale = 4)
	private BigDecimal interestRate;

	@Column(name = "effective_from", nullable = false)
	private LocalDate effectiveFrom;

	@Column(name = "effective_to")
	private LocalDate effectiveTo;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
