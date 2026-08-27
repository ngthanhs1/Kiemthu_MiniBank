package com.minibank.backend.account.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.account.dto.ClaimTransferQrRequest;
import com.minibank.backend.account.dto.CreateTransferQrRequest;
import com.minibank.backend.account.dto.TransferQrIntentResponse;
import com.minibank.backend.account.service.TransferQrIntentService;
import com.minibank.backend.common.security.CurrentJwt;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/accounts/qr-transfer-intents")
public class MobileTransferQrController {
	private final TransferQrIntentService transferQrIntentService;

	public MobileTransferQrController(TransferQrIntentService transferQrIntentService) {
		this.transferQrIntentService = transferQrIntentService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TransferQrIntentResponse create(@Valid @RequestBody CreateTransferQrRequest request) {
		long userId = CurrentJwt.requireUserId();
		return transferQrIntentService.create(userId, request.accountNumber(), request.amount());
	}

	@GetMapping("/latest/{accountNumber}")
	public TransferQrIntentResponse latest(@PathVariable String accountNumber) {
		long userId = CurrentJwt.requireUserId();
		return transferQrIntentService.getLatestForOwner(userId, accountNumber);
	}

	@PostMapping("/claim")
	public TransferQrIntentResponse claim(@Valid @RequestBody ClaimTransferQrRequest request) {
		long userId = CurrentJwt.requireUserId();
		return transferQrIntentService.claim(userId, request.intentToken());
	}
}