package com.minibank.backend.admin.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.saving.service.SavingSettlementRequestService;
import com.minibank.backend.saving.service.SavingSettlementRequestService.DecisionRequest;
import com.minibank.backend.saving.service.SavingSettlementRequestService.SettlementRequestDashboard;
import com.minibank.backend.saving.service.SavingSettlementRequestService.SettlementRequestItem;

@RestController
@RequestMapping("/api/admin/financial-products/saving-settlement-requests")
@PreAuthorize("hasAnyRole('ADMIN', 'SERVICE_OFFICER')")
public class AdminSavingSettlementController {
	private final SavingSettlementRequestService settlementRequestService;

	public AdminSavingSettlementController(SavingSettlementRequestService settlementRequestService) {
		this.settlementRequestService = settlementRequestService;
	}

	@GetMapping
	public List<SettlementRequestItem> list(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "type", required = false) String type,
		@RequestParam(value = "status", required = false) String status
	) {
		return settlementRequestService.listForAdmin(q, type, status);
	}

	@GetMapping("/dashboard")
	public SettlementRequestDashboard dashboard(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "type", required = false) String type,
		@RequestParam(value = "status", required = false) String status
	) {
		return settlementRequestService.dashboard(q, type, status);
	}

	@GetMapping("/{requestId}")
	public SettlementRequestItem detail(@PathVariable long requestId) {
		return settlementRequestService.getForAdmin(requestId);
	}

	@PostMapping("/{requestId}/approve")
	public SettlementRequestItem approve(
		@PathVariable long requestId,
		@RequestBody(required = false) DecisionRequest request
	) {
		return settlementRequestService.approve(requestId, CurrentJwt.requireUserId(), request);
	}

	@PostMapping("/{requestId}/reject")
	public SettlementRequestItem reject(
		@PathVariable long requestId,
		@RequestBody(required = false) DecisionRequest request
	) {
		return settlementRequestService.reject(requestId, CurrentJwt.requireUserId(), request);
	}
}
