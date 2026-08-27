package com.minibank.backend.contract.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;

/**
 * Tạo / cập nhật mẫu hợp đồng
 */
public record TemplateUpsertRequest(
    @NotBlank String name,
    @NotBlank String code,
    String description,
    /** "loan" | "saving" | "loan,saving" | "general" */
    @NotBlank String services,
    /** "draft" | "active" | "archived" */
    String status,
    String templateBody,
    String templateFileUrl,
    List<PlaceholderItem> placeholders
) {
    public record PlaceholderItem(
        @NotBlank String fieldCode,
        String fieldLabel,
        String dataSource,
        Integer sortOrder
    ) {}
}