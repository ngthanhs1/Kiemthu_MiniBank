package com.minibank.backend.approval.entity;

import java.math.BigDecimal;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "approval_policies")
public class ApprovalPolicy {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "service_type", nullable = false, length = 50)
	private String serviceType;

	@Column(name = "min_amount", nullable = false, precision = 18, scale = 2)
	private BigDecimal minAmount;

	@Column(name = "max_amount", precision = 18, scale = 2)
	private BigDecimal maxAmount;

	@Column(name = "staff_approvals_required", nullable = false)
	private int staffApprovalsRequired;

	@Column(name = "manager_approvals_required", nullable = false)
	private int managerApprovalsRequired;

	@Column(nullable = false)
	private boolean active;

	@Column(columnDefinition = "text")
	private String description;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
}
