package com.minibank.backend.contract.dto;

public record ContractOtpSendResponse(
    boolean devMode,
    String otp
) {}
