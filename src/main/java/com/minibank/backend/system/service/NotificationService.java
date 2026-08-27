package com.minibank.backend.system.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.system.dto.NotificationResponse;
import com.minibank.backend.system.dto.NotificationSummaryResponse;
import com.minibank.backend.system.entity.Notification;
import com.minibank.backend.system.repository.NotificationRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {
	private final NotificationRepository notificationRepository;
	private final UserRepository userRepository;
	private final SimpMessagingTemplate messagingTemplate;

	@Transactional(readOnly = true)
	public NotificationSummaryResponse summary(Long userId) {
		List<NotificationResponse> items = notificationRepository.findTop30ByUserIdOrderByCreatedAtDesc(userId)
				.stream()
				.map(NotificationResponse::from)
				.toList();
		return new NotificationSummaryResponse(notificationRepository.countByUserIdAndStatusIgnoreCase(userId, "UNREAD"), items);
	}

	@Transactional
	public NotificationResponse createForUser(Long userId, String type, String title, String content) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
		Notification notification = Notification.builder()
				.user(user)
				.channel("IN_APP")
				.type(type == null || type.isBlank() ? "SYSTEM" : type)
				.title(title)
				.content(content)
				.status("UNREAD")
				.sentAt(Instant.now())
				.build();
		NotificationResponse response = NotificationResponse.from(notificationRepository.save(notification));
		messagingTemplate.convertAndSend("/topic/notifications/" + userId, response);
		return response;
	}

	@Transactional
	public void markRead(Long userId, Long notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy thông báo"));
		if (notification.getUser() == null || !notification.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không có quyền cập nhật thông báo này");
		}
		notification.setStatus("READ");
	}
}