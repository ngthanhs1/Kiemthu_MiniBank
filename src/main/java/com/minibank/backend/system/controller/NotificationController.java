package com.minibank.backend.system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.system.dto.NotificationSummaryResponse;
import com.minibank.backend.system.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mobile/notifications")
@RequiredArgsConstructor
public class NotificationController {
	private final NotificationService notificationService;

	@GetMapping
	public NotificationSummaryResponse list() {
		return notificationService.summary(CurrentJwt.requireUserId());
	}

	@PatchMapping("/{id}/read")
	public void markRead(@PathVariable Long id) {
		notificationService.markRead(CurrentJwt.requireUserId(), id);
	}
}