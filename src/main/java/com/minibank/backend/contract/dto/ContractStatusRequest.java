package com.minibank.backend.contract.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body để cập nhật trạng thái hợp đồng.
 * PUT /api/admin/contracts/{id}/status
 */
public record ContractStatusRequest(
    @NotBlank String status   // SENT | PENDING_SIGNATURE | SIGNED | CANCELLED
) {}