package com.minibank.backend.approval.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_instances")
public class ApprovalInstance {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "service_type", nullable = false, length = 50)
	private String serviceType;

	@Column(name = "target_id", nullable = false)
	private Long targetId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approval_policy_id")
	private ApprovalPolicy approvalPolicy;

	@Column(nullable = false, length = 32)
	private String status;

	@Column(name = "current_stage", nullable = false, length = 32)
	private String currentStage;

	@Column(name = "staff_approvals_required", nullable = false)
	private int staffApprovalsRequired;

	@Column(name = "manager_approvals_required", nullable = false)
	private int managerApprovalsRequired;

	@Column(name = "staff_approved_count", nullable = false)
	private int staffApprovedCount;

	@Column(name = "manager_approved_count", nullable = false)
	private int managerApprovedCount;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "completed_at")
	private Instant completedAt;
}
