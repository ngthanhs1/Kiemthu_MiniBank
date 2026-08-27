package com.minibank.backend.transaction.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.transaction.dto.TransferConfirmRequest;
import com.minibank.backend.transaction.dto.TransferConfirmResponse;
import com.minibank.backend.transaction.dto.TransferInitiateRequest;
import com.minibank.backend.transaction.dto.TransferInitiateResponse;
import com.minibank.backend.transaction.service.TransferService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/transfers")
public class MobileTransferController {
	private final TransferService transferService;

	public MobileTransferController(TransferService transferService) {
		this.transferService = transferService;
	}

	@PostMapping("/initiate")
	public TransferInitiateResponse initiate(@Valid @RequestBody TransferInitiateRequest request) {
		long userId = CurrentJwt.requireUserId();
		return transferService.initiate(userId, request);
	}

	@PostMapping("/confirm")
	public TransferConfirmResponse confirm(@Valid @RequestBody TransferConfirmRequest request) {
		long userId = CurrentJwt.requireUserId();
		return transferService.confirm(userId, request);
	}
}
