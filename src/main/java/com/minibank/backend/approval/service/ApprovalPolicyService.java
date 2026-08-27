package com.minibank.backend.approval.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.approval.dto.ApprovalPolicyRequest;
import com.minibank.backend.approval.dto.ApprovalPolicyResponse;
import com.minibank.backend.approval.entity.ApprovalPolicy;
import com.minibank.backend.approval.repository.ApprovalPolicyRepository;

@Service
public class ApprovalPolicyService {
	private final ApprovalPolicyRepository policyRepository;

	public ApprovalPolicyService(ApprovalPolicyRepository policyRepository) {
		this.policyRepository = policyRepository;
	}

	@Transactional(readOnly = true)
	public List<ApprovalPolicyResponse> list() {
		return policyRepository.findAllByOrderByServiceTypeAscMinAmountAsc().stream().map(this::toResponse).toList();
	}

	@Transactional
	public ApprovalPolicyResponse create(ApprovalPolicyRequest request) {
		ApprovalPolicy policy = new ApprovalPolicy();
		apply(policy, request);
		return toResponse(policyRepository.save(policy));
	}

	@Transactional
	public ApprovalPolicyResponse update(long id, ApprovalPolicyRequest request) {
		ApprovalPolicy policy = policyRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval policy not found"));
		apply(policy, request);
		return toResponse(policyRepository.save(policy));
	}

	@Transactional
	public void delete(long id) {
		if (!policyRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval policy not found");
		}
		policyRepository.deleteById(id);
	}

	private void apply(ApprovalPolicy policy, ApprovalPolicyRequest request) {
		String serviceType = requireText(request.serviceType(), "serviceType").toLowerCase();
		if (!serviceType.equals("saving") && !serviceType.equals("loan_application")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "serviceType must be saving or loan_application");
		}
		BigDecimal minAmount = request.minAmount() == null ? BigDecimal.ZERO : request.minAmount();
		if (minAmount.compareTo(BigDecimal.ZERO) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minAmount must be >= 0");
		}
		if (request.maxAmount() != null && request.maxAmount().compareTo(minAmount) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxAmount must be >= minAmount");
		}
		int staffRequired = request.staffApprovalsRequired() == null ? 1 : request.staffApprovalsRequired();
		int managerRequired = request.managerApprovalsRequired() == null ? 0 : request.managerApprovalsRequired();
		if (staffRequired < 0 || managerRequired < 0 || staffRequired + managerRequired <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one approval is required");
		}

		policy.setServiceType(serviceType);
		policy.setMinAmount(minAmount);
		policy.setMaxAmount(request.maxAmount());
		policy.setStaffApprovalsRequired(staffRequired);
		policy.setManagerApprovalsRequired(managerRequired);
		policy.setActive(request.active() == null || request.active());
		policy.setDescription(request.description());
	}

	private String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " is required");
		}
		return value.trim();
	}

	private ApprovalPolicyResponse toResponse(ApprovalPolicy policy) {
		return new ApprovalPolicyResponse(
			policy.getId(),
			policy.getServiceType(),
			policy.getMinAmount(),
			policy.getMaxAmount(),
			policy.getStaffApprovalsRequired(),
			policy.getManagerApprovalsRequired(),
			policy.isActive(),
			policy.getDescription(),
			policy.getCreatedAt(),
			policy.getUpdatedAt()
		);
	}
}
