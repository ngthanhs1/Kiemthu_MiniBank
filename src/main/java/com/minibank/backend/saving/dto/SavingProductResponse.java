package com.minibank.backend.saving.dto;
 
import java.math.BigDecimal;

import com.minibank.backend.saving.entity.SavingProduct;
 
/**
 * Lightweight response DTO for saving product listing on mobile.
 */
public record SavingProductResponse(
        Long id,
        String code,
        String name,
        String currency,
        String termUnit,
        int termValue,
        String interestRateType,
        BigDecimal baseInterestRate,
        BigDecimal penaltyInterestRate,
        String interestAccrualFrequency,
        String interestPostingFrequency,
        boolean capitalized,
        BigDecimal minOpenAmount,
        BigDecimal maxOpenAmount,
        String status
) {
    public static SavingProductResponse from(SavingProduct p) {
        return new SavingProductResponse(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getCurrency(),
                p.getTermUnit(),
                p.getTermValue(),
                p.getInterestRateType(),
                p.getBaseInterestRate(),
                p.getPenaltyInterestRate(),
                p.getInterestAccrualFrequency(),
                p.getInterestPostingFrequency(),
                p.isCapitalized(),
                p.getMinOpenAmount(),
                p.getMaxOpenAmount(),
                p.getStatus()
        );
    }
}