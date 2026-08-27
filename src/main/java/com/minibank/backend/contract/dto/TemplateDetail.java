package com.minibank.backend.contract.dto;

import java.util.List;

public record TemplateDetail(
    Long id,
    String name,
    String code,
    String description,
    String services,
    String status,
    String templateBody,
    String templateFileUrl,
    List<PlaceholderDetail> placeholders,
    String createdAt,
    String updatedAt
) {
    public record PlaceholderDetail(
        Long id,
        String fieldCode,
        String fieldLabel,
        String dataSource,
        int sortOrder
    ) {}
}
