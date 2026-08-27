package com.minibank.backend.admin.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.approval.dto.ApprovalPolicyRequest;
import com.minibank.backend.approval.dto.ApprovalPolicyResponse;
import com.minibank.backend.approval.service.ApprovalPolicyService;

@RestController
@RequestMapping("/api/admin/system/approval-policies")
@PreAuthorize("hasRole('ADMIN')")
public class AdminApprovalPolicyController {
	private final ApprovalPolicyService policyService;

	public AdminApprovalPolicyController(ApprovalPolicyService policyService) {
		this.policyService = policyService;
	}

	@GetMapping
	public List<ApprovalPolicyResponse> list() {
		return policyService.list();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApprovalPolicyResponse create(@RequestBody ApprovalPolicyRequest request) {
		return policyService.create(request);
	}

	@PutMapping("/{id}")
	public ApprovalPolicyResponse update(@PathVariable long id, @RequestBody ApprovalPolicyRequest request) {
		return policyService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long id) {
		policyService.delete(id);
	}
}
