package com.minibank.backend.approval.dto;

import java.util.List;

public record ApprovalProgressResponse(
	Long instanceId,
	String status,
	String currentStage,
	int staffApprovalsRequired,
	int staffApprovedCount,
	int managerApprovalsRequired,
	int managerApprovedCount,
	boolean finalApproved,
	boolean rejected,
	List<ApprovalActionItem> actions
) {
	public record ApprovalActionItem(
		Long id,
		Long adminUserId,
		String adminFullName,
		String approverRole,
		String action,
		String note,
		String actedAt
	) {}
}
