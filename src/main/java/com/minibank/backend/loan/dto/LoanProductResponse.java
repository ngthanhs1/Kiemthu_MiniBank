package com.minibank.backend.loan.dto;

import java.math.BigDecimal;

public record LoanProductResponse(
    Long id,
    String code,
    String name,
    String loanType,
    String currency,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    int minTermMonths,
    int maxTermMonths,
    String interestRateType,
    BigDecimal baseInterestRate,
    BigDecimal penaltyInterestRate,
    String interestCalculationMethod,
    String repaymentFrequency,
    String status
) {}