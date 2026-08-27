package com.minibank.backend.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body khi người dùng mobile chấp nhận hoặc từ chối hợp đồng.
 *
 * referenceType: "saving" | "loan_application"
 * referenceId  : ID của saving hoặc loan_application
 * templateCode : "SAVING_AGREEMENT" | "LOAN_CREDIT" | "LOAN_MORTGAGE"
 * signatureData: (tuỳ chọn) chuỗi base64 ảnh chữ ký tay nếu app hỗ trợ vẽ chữ ký
 * otpCode      : OTP xác nhận chữ ký điện tử
 */
public record ContractAcceptRequest(
        @NotBlank String referenceType,
        @NotNull  Long   referenceId,
        @NotBlank String templateCode,
        String signatureData,
        String otpCode
) {}
