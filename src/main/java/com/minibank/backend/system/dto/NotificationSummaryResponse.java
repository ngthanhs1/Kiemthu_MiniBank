package com.minibank.backend.system.dto;

import java.util.List;

public record NotificationSummaryResponse(long unreadCount, List<NotificationResponse> notifications) {
}