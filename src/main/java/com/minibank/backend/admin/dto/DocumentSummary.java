package com.minibank.backend.admin.dto;

import java.time.Instant;

public record DocumentSummary(
	Long id,
	String ownerType,
	Long ownerId,
	String documentType,
	String fileName,
	String fileUrl,
	String mimeType,
	String verifiedStatus,
	String uploadedByType,
	Long uploadedById,
	Instant uploadedAt,
	Long verifiedById,
	Instant verifiedAt,
	String note
) {}
