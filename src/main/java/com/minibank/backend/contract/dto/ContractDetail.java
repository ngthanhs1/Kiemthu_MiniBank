package com.minibank.backend.contract.dto;

public record ContractDetail(
    Long id,
    String contractNumber,
    Long templateId,
    String templateName,
    String ownerType,
    Long ownerId,
    String status,
    String fileUrl,
    String renderedBody,
    String signedAt,
    String createdAt
) {}
