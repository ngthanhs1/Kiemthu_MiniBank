package com.minibank.backend.admin.dto;

import java.math.BigDecimal;

public record UpdateAccountLimitRequest(
        BigDecimal dailyTransferLimit,
        BigDecimal dailyReceiveLimit) {
}