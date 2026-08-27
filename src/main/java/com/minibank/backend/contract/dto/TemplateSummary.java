package com.minibank.backend.contract.dto;

public record TemplateSummary(
    Long id,
    String name,
    String code,
    String description,
    String services,
    String status,
    String templateBody,
    String templateFileUrl,
    int placeholderCount,
    String createdAt,
    String updatedAt
) {}
