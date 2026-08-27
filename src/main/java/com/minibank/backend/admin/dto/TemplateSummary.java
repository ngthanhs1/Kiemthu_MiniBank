package com.minibank.backend.admin.dto;

public record TemplateSummary(
    Long id,
    String name,
    String code,
    String description,
    String templateFileUrl,
    String createdAt
) {}
