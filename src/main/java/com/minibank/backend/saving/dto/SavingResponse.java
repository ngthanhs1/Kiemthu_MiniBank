package com.minibank.backend.saving.dto;

import java.math.BigDecimal;
import java.time.Instant;

import com.minibank.backend.approval.dto.ApprovalProgressResponse;
import com.minibank.backend.saving.entity.Saving;

public record SavingResponse(
    Long id,
    String code,
    String status,
    String userFullName,
    String userPhone,
    BigDecimal principalAmount,
    BigDecimal actualInterestRate,
    String productName,
    String termUnit,
    int termValue,
    String sourceAccountNumber,
    String sourceAccountName,
    boolean autoRenew,
    String openDate,
    String maturityDate,
    Instant createdAt,
    ApprovalProgressResponse approvalProgress
) {
    public static SavingResponse from(Saving s) {
        return from(s, null);
    }

    public static SavingResponse from(Saving s, ApprovalProgressResponse approvalProgress) {
        return new SavingResponse(
            s.getId(),
            s.getCode(),
            s.getStatus(),
            s.getUser() != null ? s.getUser().getFullName() : null,
            s.getUser() != null ? s.getUser().getPhone() : null,
            s.getPrincipalAmount(),
            s.getActualInterestRate(),
            s.getSavingProduct() != null ? s.getSavingProduct().getName() : null,
            s.getTermUnit(),
            s.getTermValue(),
            s.getSourceAccount() != null ? s.getSourceAccount().getAccountNumber() : null,
            s.getSourceAccount() != null ? s.getSourceAccount().getAccountName() : null,
            s.isAutoRenew(),
            s.getOpenDate() != null ? s.getOpenDate().toString() : null,
            s.getMaturityDate() != null ? s.getMaturityDate().toString() : null,
            s.getCreatedAt(),
            approvalProgress
        );
    }
}
