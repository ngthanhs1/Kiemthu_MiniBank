package com.minibank.backend.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ContractGenerateRequest(
    @NotNull Long templateId,
    /** "USER" | "loan_application" | "saving" */
    @NotBlank String ownerType,
    @NotNull Long ownerId,
    String contractNumber,
    Map<String, String> dataOverrides
) {}
