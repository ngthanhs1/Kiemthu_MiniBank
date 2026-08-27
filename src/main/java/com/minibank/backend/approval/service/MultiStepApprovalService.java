package com.minibank.backend.approval.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.entity.AdminUser;
import com.minibank.backend.admin.repository.AdminUserRepository;
import com.minibank.backend.admin.repository.AdminUserRoleRepository;
import com.minibank.backend.admin.repository.RoleRepository;
import com.minibank.backend.approval.dto.ApprovalProgressResponse;
import com.minibank.backend.approval.entity.ApprovalAction;
import com.minibank.backend.approval.entity.ApprovalInstance;
import com.minibank.backend.approval.entity.ApprovalPolicy;
import com.minibank.backend.approval.repository.ApprovalActionRepository;
import com.minibank.backend.approval.repository.ApprovalInstanceRepository;
import com.minibank.backend.approval.repository.ApprovalPolicyRepository;

@Service
public class MultiStepApprovalService {
	private static final String STATUS_PENDING = "PENDING";
	private static final String STATUS_APPROVED = "APPROVED";
	private static final String STATUS_REJECTED = "REJECTED";
	private static final String STAGE_STAFF = "STAFF";
	private static final String STAGE_MANAGER = "MANAGER";

	private final ApprovalPolicyRepository policyRepository;
	private final ApprovalInstanceRepository instanceRepository;
	private final ApprovalActionRepository actionRepository;
	private final AdminUserRepository adminUserRepository;
	private final AdminUserRoleRepository adminUserRoleRepository;
	private final RoleRepository roleRepository;

	public MultiStepApprovalService(
		ApprovalPolicyRepository policyRepository,
		ApprovalInstanceRepository instanceRepository,
		ApprovalActionRepository actionRepository,
		AdminUserRepository adminUserRepository,
		AdminUserRoleRepository adminUserRoleRepository,
		RoleRepository roleRepository
	) {
		this.policyRepository = policyRepository;
		this.instanceRepository = instanceRepository;
		this.actionRepository = actionRepository;
		this.adminUserRepository = adminUserRepository;
		this.adminUserRoleRepository = adminUserRoleRepository;
		this.roleRepository = roleRepository;
	}

	@Transactional
	public ApprovalProgressResponse approve(String serviceType, Long targetId, BigDecimal amount, Long adminUserId, String note) {
		ApprovalInstance instance = getOrCreate(serviceType, targetId, amount);
		if (STATUS_APPROVED.equalsIgnoreCase(instance.getStatus())) {
			return toProgress(instance);
		}
		if (STATUS_REJECTED.equalsIgnoreCase(instance.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval request is rejected");
		}
		if (actionRepository.existsByApprovalInstanceIdAndAdminUserIdAndActionIgnoreCase(instance.getId(), adminUserId, STATUS_APPROVED)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "This admin already approved this request");
		}

		AdminUser admin = adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));
		Set<String> roles = loadRoles(adminUserId);
		Set<String> permissions = loadPermissions(adminUserId);
		String stage = instance.getCurrentStage();
		ensureNotConsecutiveSameApprover(instance, adminUserId);
		String approverRole = resolveApproverRole(stage, roles, permissions, instance.getServiceType());

		ApprovalAction action = new ApprovalAction();
		action.setApprovalInstance(instance);
		action.setAdminUser(admin);
		action.setApproverRole(approverRole);
		action.setAction(STATUS_APPROVED);
		action.setNote(note);
		actionRepository.save(action);

		if (STAGE_MANAGER.equals(stage)) {
			instance.setManagerApprovedCount(instance.getManagerApprovedCount() + 1);
		} else {
			instance.setStaffApprovedCount(instance.getStaffApprovedCount() + 1);
		}
		advance(instance);
		return toProgress(instanceRepository.save(instance));
	}

	@Transactional
	public ApprovalProgressResponse reject(String serviceType, Long targetId, BigDecimal amount, Long adminUserId, String note) {
		ApprovalInstance instance = getOrCreate(serviceType, targetId, amount);
		if (STATUS_APPROVED.equalsIgnoreCase(instance.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval request is already approved");
		}
		AdminUser admin = adminUserRepository.findById(adminUserId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin not found"));
		String stage = instance.getCurrentStage();
		Set<String> roles = loadRoles(adminUserId);
		Set<String> permissions = loadPermissions(adminUserId);

		ApprovalAction action = new ApprovalAction();
		action.setApprovalInstance(instance);
		action.setAdminUser(admin);
		action.setApproverRole(resolveApproverRole(stage, roles, permissions, instance.getServiceType()));
		action.setAction(STATUS_REJECTED);
		action.setNote(note);
		actionRepository.save(action);

		instance.setStatus(STATUS_REJECTED);
		instance.setCompletedAt(Instant.now());
		return toProgress(instanceRepository.save(instance));
	}

	@Transactional
	public ApprovalInstance getOrCreate(String serviceType, Long targetId, BigDecimal amount) {
		return instanceRepository.findByServiceTypeIgnoreCaseAndTargetId(serviceType, targetId)
			.orElseGet(() -> createInstance(serviceType, targetId, amount == null ? BigDecimal.ZERO : amount));
	}

	@Transactional(readOnly = true)
	public ApprovalProgressResponse findProgress(String serviceType, Long targetId) {
		return instanceRepository.findByServiceTypeIgnoreCaseAndTargetId(serviceType, targetId)
			.map(this::toProgress)
			.orElse(null);
	}

	public ApprovalProgressResponse toProgress(ApprovalInstance instance) {
		List<ApprovalProgressResponse.ApprovalActionItem> actions = actionRepository
			.findByApprovalInstanceIdOrderByActedAtAsc(instance.getId())
			.stream()
			.map(action -> new ApprovalProgressResponse.ApprovalActionItem(
				action.getId(),
				action.getAdminUser() != null ? action.getAdminUser().getId() : null,
				action.getAdminUser() != null ? action.getAdminUser().getFullName() : null,
				action.getApproverRole(),
				action.getAction(),
				action.getNote(),
				action.getActedAt() != null ? action.getActedAt().toString() : null
			))
			.toList();
		return new ApprovalProgressResponse(
			instance.getId(),
			instance.getStatus(),
			instance.getCurrentStage(),
			instance.getStaffApprovalsRequired(),
			instance.getStaffApprovedCount(),
			instance.getManagerApprovalsRequired(),
			instance.getManagerApprovedCount(),
			STATUS_APPROVED.equalsIgnoreCase(instance.getStatus()),
			STATUS_REJECTED.equalsIgnoreCase(instance.getStatus()),
			actions
		);
	}

	private ApprovalInstance createInstance(String serviceType, Long targetId, BigDecimal amount) {
		ApprovalPolicy policy = policyRepository.findBestMatch(serviceType, amount).orElse(null);
		int staffRequired = policy != null ? policy.getStaffApprovalsRequired() : 1;
		int managerRequired = policy != null ? policy.getManagerApprovalsRequired() : 0;

		ApprovalInstance instance = new ApprovalInstance();
		instance.setServiceType(serviceType.toLowerCase(Locale.ROOT));
		instance.setTargetId(targetId);
		instance.setApprovalPolicy(policy);
		instance.setStatus(STATUS_PENDING);
		instance.setCurrentStage(staffRequired > 0 ? STAGE_STAFF : STAGE_MANAGER);
		instance.setStaffApprovalsRequired(staffRequired);
		instance.setManagerApprovalsRequired(managerRequired);
		instance.setStaffApprovedCount(0);
		instance.setManagerApprovedCount(0);
		advance(instance);
		return instanceRepository.save(instance);
	}

	private void advance(ApprovalInstance instance) {
		if (instance.getStaffApprovedCount() < instance.getStaffApprovalsRequired()) {
			instance.setCurrentStage(STAGE_STAFF);
			instance.setStatus(STATUS_PENDING);
			return;
		}
		if (instance.getManagerApprovedCount() < instance.getManagerApprovalsRequired()) {
			instance.setCurrentStage(STAGE_MANAGER);
			instance.setStatus(STATUS_PENDING);
			return;
		}
		instance.setStatus(STATUS_APPROVED);
		instance.setCompletedAt(Instant.now());
	}

	private String resolveApproverRole(String stage, Set<String> roles, Set<String> permissions, String serviceType) {
		if (STAGE_MANAGER.equalsIgnoreCase(stage)) {
			if (roles.contains("ADMIN") || roles.contains("SUPER_ADMIN") || roles.contains("MANAGER")) return STAGE_MANAGER;
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Manager approval is required");
		}
		if (roles.contains("STAFF")) return STAGE_STAFF;
		if ("LOAN_APPLICATION".equalsIgnoreCase(serviceType)
			&& (roles.contains("LOAN_OFFICER") || hasPermission(permissions, "LOAN_APPLICATION_APPROVAL"))) return STAGE_STAFF;
		if ("SAVING".equalsIgnoreCase(serviceType)
			&& (roles.contains("SERVICE_OFFICER") || hasPermission(permissions, "SAVING_APPROVAL"))) return STAGE_STAFF;
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Staff approval is required");
	}

	private Set<String> loadRoles(Long adminUserId) {
		return adminUserRoleRepository.findRoleCodesByAdminUserId(adminUserId)
			.stream()
			.map(role -> role == null ? "" : role.trim().toUpperCase(Locale.ROOT))
			.filter(role -> !role.isBlank())
			.collect(java.util.stream.Collectors.toSet());
	}

	private Set<String> loadPermissions(Long adminUserId) {
		return adminUserRoleRepository.findRoleCodesByAdminUserId(adminUserId)
			.stream()
			.map(roleRepository::findByCode)
			.flatMap(java.util.Optional::stream)
			.map(role -> role.getPermissionsJson() == null ? "" : role.getPermissionsJson())
			.flatMap(value -> java.util.Arrays.stream(value.split("\\r?\\n")))
			.map(permission -> permission == null ? "" : permission.trim().toUpperCase(Locale.ROOT))
			.filter(permission -> !permission.isBlank())
			.collect(java.util.stream.Collectors.toSet());
	}

	private boolean hasPermission(Set<String> permissions, String permissionCode) {
		if (permissions == null || permissionCode == null) {
			return false;
		}
		String normalized = permissionCode.trim().toUpperCase(Locale.ROOT);
		return !normalized.isBlank() && permissions.stream().anyMatch(normalized::equals);
	}

	private void ensureNotConsecutiveSameApprover(ApprovalInstance instance, Long adminUserId) {
		ApprovalAction previousApproved = actionRepository.findByApprovalInstanceIdOrderByActedAtAsc(instance.getId())
			.stream()
			.filter(action -> STATUS_APPROVED.equalsIgnoreCase(action.getAction()))
			.reduce((left, right) -> right)
			.orElse(null);
		if (previousApproved != null
			&& previousApproved.getAdminUser() != null
			&& previousApproved.getAdminUser().getId() != null
			&& previousApproved.getAdminUser().getId().equals(adminUserId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Consecutive approvals by the same admin are not allowed");
		}
	}
}
