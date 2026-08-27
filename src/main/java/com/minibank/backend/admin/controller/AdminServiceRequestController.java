package com.minibank.backend.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.admin.dto.AdminServiceRequestDecisionRequest;
import com.minibank.backend.admin.dto.AdminServiceRequestDetail;
import com.minibank.backend.admin.dto.AdminServiceRequestSummary;
import com.minibank.backend.admin.service.AdminServiceRequestService;
import com.minibank.backend.common.security.CurrentJwt;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/service-requests")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'Xem yêu cầu dịch vụ')")
public class AdminServiceRequestController {
	private final AdminServiceRequestService adminServiceRequestService;

	public AdminServiceRequestController(AdminServiceRequestService adminServiceRequestService) {
		this.adminServiceRequestService = adminServiceRequestService;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<AdminServiceRequestSummary> list(
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "type", required = false) String type
	) {
		return adminServiceRequestService.list(status, type);
	}

	@GetMapping("/{id}")
	@Transactional(readOnly = true)
	public AdminServiceRequestDetail get(@PathVariable Long id) {
		return adminServiceRequestService.getDetail(id);
	}

	@PostMapping("/{id}/approve")
	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'Duyệt yêu cầu dịch vụ')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void approve(@PathVariable Long id, @Valid @RequestBody AdminServiceRequestDecisionRequest request) {
		long adminUserId = CurrentJwt.requireUserId();
		adminServiceRequestService.approve(id, adminUserId, request.note());
	}

	@PostMapping("/{id}/reject")
	@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'Từ chối yêu cầu dịch vụ')")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reject(@PathVariable Long id, @Valid @RequestBody AdminServiceRequestDecisionRequest request) {
		long adminUserId = CurrentJwt.requireUserId();
		adminServiceRequestService.reject(id, adminUserId, request.note());
	}
}
