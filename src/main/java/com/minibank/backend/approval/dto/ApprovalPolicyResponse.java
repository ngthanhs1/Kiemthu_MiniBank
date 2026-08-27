package com.minibank.backend.approval.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ApprovalPolicyResponse(
	Long id,
	String serviceType,
	BigDecimal minAmount,
	BigDecimal maxAmount,
	int staffApprovalsRequired,
	int managerApprovalsRequired,
	boolean active,
	String description,
	Instant createdAt,
	Instant updatedAt
) {}
