package com.minibank.backend.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentUploadRequest(
    @NotBlank String documentType,
    String fileName,
    @NotBlank String fileUrl,
    String mimeType,
    String note
) {}
