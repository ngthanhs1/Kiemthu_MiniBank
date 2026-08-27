package com.minibank.backend.approval.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.approval.entity.ApprovalAction;

public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {
	boolean existsByApprovalInstanceIdAndAdminUserIdAndActionIgnoreCase(Long approvalInstanceId, Long adminUserId, String action);
	List<ApprovalAction> findByApprovalInstanceIdOrderByActedAtAsc(Long approvalInstanceId);
}
