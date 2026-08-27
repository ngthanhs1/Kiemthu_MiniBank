package com.minibank.backend.loan.dto;

import java.time.Instant;

public record MobileContractResponse(
    Long id,
    String ownerType,
    Long ownerId,
    String contractNumber,
    String status,
    Instant signedAt,
    Instant createdAt,
    String fileUrl,
    String renderedBody
) {}