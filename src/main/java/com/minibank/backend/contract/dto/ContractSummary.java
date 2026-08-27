package com.minibank.backend.contract.dto;

public record ContractSummary(
    Long id,
    String contractNumber,
    Long templateId,
    String templateName,
    String ownerType,
    Long ownerId,
    String status,
    String fileUrl,
    String createdAt
) {}
