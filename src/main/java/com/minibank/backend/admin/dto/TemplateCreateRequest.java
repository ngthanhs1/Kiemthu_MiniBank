package com.minibank.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemplateCreateRequest(
    @NotBlank String name,
    @NotBlank String code,
    @Size(max = 2000) String description,
    @NotBlank String templateFileUrl
) {}
