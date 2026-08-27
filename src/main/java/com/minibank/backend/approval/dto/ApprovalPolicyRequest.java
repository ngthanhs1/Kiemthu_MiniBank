package com.minibank.backend.approval.dto;

import java.math.BigDecimal;

public record ApprovalPolicyRequest(
	String serviceType,
	BigDecimal minAmount,
	BigDecimal maxAmount,
	Integer staffApprovalsRequired,
	Integer managerApprovalsRequired,
	Boolean active,
	String description
) {}
