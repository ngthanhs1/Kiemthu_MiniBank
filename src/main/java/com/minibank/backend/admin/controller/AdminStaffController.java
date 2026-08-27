package com.minibank.backend.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.admin.dto.AdminStaffRequest;
import com.minibank.backend.admin.dto.AdminStaffResponse;
import com.minibank.backend.admin.dto.AdminStaffRolesRequest;
import com.minibank.backend.admin.dto.AdminStaffStatusRequest;
import com.minibank.backend.admin.service.AdminStaffService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStaffController {
	private final AdminStaffService adminStaffService;

	public AdminStaffController(AdminStaffService adminStaffService) {
		this.adminStaffService = adminStaffService;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<AdminStaffResponse> list(@RequestParam(value = "q", required = false) String q) {
		return adminStaffService.list(q);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public AdminStaffResponse create(@Valid @RequestBody AdminStaffRequest request) {
		return adminStaffService.create(request);
	}

	@PutMapping("/{adminUserId}/roles")
	@Transactional
	public AdminStaffResponse updateRoles(
		@PathVariable Long adminUserId,
		@RequestBody AdminStaffRolesRequest request
	) {
		return adminStaffService.updateRoles(adminUserId, request.roles());
	}

	@PutMapping("/{adminUserId}/status")
	@Transactional
	public AdminStaffResponse updateStatus(
		@PathVariable Long adminUserId,
		@Valid @RequestBody AdminStaffStatusRequest request
	) {
		return adminStaffService.updateStatus(adminUserId, request.status());
	}
}
