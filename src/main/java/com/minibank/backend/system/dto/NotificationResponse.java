package com.minibank.backend.system.dto;

import java.time.Instant;

import com.minibank.backend.system.entity.Notification;

public record NotificationResponse(
		Long id,
		String channel,
		String type,
		String title,
		String content,
		String status,
		Instant createdAt,
		Instant sentAt) {
	public static NotificationResponse from(Notification notification) {
		return new NotificationResponse(
				notification.getId(),
				notification.getChannel(),
				notification.getType(),
				notification.getTitle(),
				notification.getContent(),
				notification.getStatus(),
				notification.getCreatedAt(),
				notification.getSentAt());
	}
}