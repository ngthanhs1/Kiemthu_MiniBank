package com.minibank.backend.approval.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import com.minibank.backend.admin.entity.AdminUser;

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
@Table(name = "approval_actions")
public class ApprovalAction {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "approval_instance_id", nullable = false)
	private ApprovalInstance approvalInstance;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "admin_user_id", nullable = false)
	private AdminUser adminUser;

	@Column(name = "approver_role", nullable = false, length = 32)
	private String approverRole;

	@Column(nullable = false, length = 32)
	private String action;

	@Column(columnDefinition = "text")
	private String note;

	@CreationTimestamp
	@Column(name = "acted_at", nullable = false, updatable = false)
	private Instant actedAt;
}
