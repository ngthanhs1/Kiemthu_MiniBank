package com.minibank.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContractGenerateRequest(
    @NotBlank String ownerType,
    @NotNull Long ownerId,
    @NotNull Long templateId,
    String contractNumber
) {}
