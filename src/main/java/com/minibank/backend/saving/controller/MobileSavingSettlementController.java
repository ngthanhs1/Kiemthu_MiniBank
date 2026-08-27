package com.minibank.backend.saving.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.saving.service.SavingSettlementRequestService;
import com.minibank.backend.saving.service.SavingSettlementRequestService.CreateSettlementRequest;
import com.minibank.backend.saving.service.SavingSettlementRequestService.SettlementRequestItem;

@RestController
@RequestMapping("/api/mobile/savings/settlement-requests")
public class MobileSavingSettlementController {
	private final SavingSettlementRequestService settlementRequestService;

	public MobileSavingSettlementController(SavingSettlementRequestService settlementRequestService) {
		this.settlementRequestService = settlementRequestService;
	}

	@GetMapping
	public List<SettlementRequestItem> list() {
		return settlementRequestService.listForUser(CurrentJwt.requireUserId());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SettlementRequestItem create(@RequestBody CreateSettlementRequest request) {
		return settlementRequestService.create(CurrentJwt.requireUserId(), request);
	}
}
