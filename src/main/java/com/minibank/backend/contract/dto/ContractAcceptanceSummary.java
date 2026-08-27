package com.minibank.backend.contract.dto;

public record ContractAcceptanceSummary(
    String agreementType,
    String referenceType,
    Long referenceId,
    Long userId,
    String userFullName,
    String userPhone,
    Long templateId,
    String templateCode,
    String templateName,
    String templateVersion,
    String contractNumber,
    String acceptanceStatus,
    String acceptedAt
) {}
