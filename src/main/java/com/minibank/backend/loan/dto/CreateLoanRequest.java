package com.minibank.backend.loan.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateLoanRequest(
    @NotNull Long loanProductId,
    @NotNull Long disbursementAccountId,
    @NotNull Long repaymentAccountId,

    @NotNull @Positive BigDecimal amount,
    @NotNull @Min(1) @Max(360) Integer termMonths,
    @NotBlank String purpose,

    // Thêm các field Flutter đang gửi
    String loanType,
    BigDecimal monthlyIncome,
    String collateralDescription,
    Double collateralEstimatedValue,
    String incomeProofUrl,
    String collateralProofUrl,
    String bankStatementUrl,
    String workCertUrl,
    String maritalStatus,
    Integer numberOfDependents,
    String education,
    String occupation,
    String workDuration,
    String housingStatus,
    String mailingAddress
) {}