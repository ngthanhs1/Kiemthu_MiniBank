package com.minibank.backend.admin.dto;

public record ContractSummary(
    Long id,
    String ownerType,
    Long ownerId,
    String contractNumber,
    String status,
    String fileUrl,
    String createdAt
) {}
