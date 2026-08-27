package com.minibank.backend.contract.dto;

/**
 * Kết quả trả về sau khi người dùng ký hợp đồng thành công.
 */
public record ContractAcceptResult(
        String contractNumber,
        String status,
        String fileUrl,
        String signedAt
) {}