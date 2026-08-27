package com.minibank.backend.loan.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanApplicationResponse(
    Long id,
    String productName,
    BigDecimal requestedAmount,
    int requestedTermMonths,
    String purpose,
    String status,
    String priorityTag,
    Instant submittedAt
) {}