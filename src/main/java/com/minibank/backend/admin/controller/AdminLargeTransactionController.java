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

import com.minibank.backend.admin.dto.LargeTransactionDetail;
import com.minibank.backend.admin.dto.LargeTransactionSummary;
import com.minibank.backend.admin.dto.TransactionDecisionRequest;
import com.minibank.backend.admin.service.AdminLargeTransactionService;
import com.minibank.backend.common.security.CurrentJwt;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/transactions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLargeTransactionController {
	private final AdminLargeTransactionService adminLargeTransactionService;

	public AdminLargeTransactionController(AdminLargeTransactionService adminLargeTransactionService) {
		this.adminLargeTransactionService = adminLargeTransactionService;
	}

	@GetMapping("/large")
	@Transactional(readOnly = true)
	public List<LargeTransactionSummary> list(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "risk", required = false) String risk
	) {
		return adminLargeTransactionService.list(q, status, risk);
	}

	@GetMapping("/large/{transactionId}")
	@Transactional(readOnly = true)
	public LargeTransactionDetail detail(@PathVariable Long transactionId) {
		return adminLargeTransactionService.detail(transactionId);
	}

	@PostMapping("/large/{transactionId}/approve")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void approve(
		@PathVariable Long transactionId,
		@Valid @RequestBody TransactionDecisionRequest request
	) {
		long adminUserId = CurrentJwt.requireUserId();
		adminLargeTransactionService.approve(transactionId, adminUserId, request.note());
	}

	@PostMapping("/large/{transactionId}/reject")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void reject(
		@PathVariable Long transactionId,
		@Valid @RequestBody TransactionDecisionRequest request
	) {
		long adminUserId = CurrentJwt.requireUserId();
		adminLargeTransactionService.reject(transactionId, adminUserId, request.note());
	}
}
