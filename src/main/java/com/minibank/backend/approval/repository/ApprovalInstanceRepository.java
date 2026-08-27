package com.minibank.backend.approval.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.approval.entity.ApprovalInstance;

public interface ApprovalInstanceRepository extends JpaRepository<ApprovalInstance, Long> {
	Optional<ApprovalInstance> findByServiceTypeIgnoreCaseAndTargetId(String serviceType, Long targetId);
}
