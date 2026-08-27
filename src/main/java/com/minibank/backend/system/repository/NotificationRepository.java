package com.minibank.backend.system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.minibank.backend.system.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	List<Notification> findTop30ByUserIdOrderByCreatedAtDesc(Long userId);

	long countByUserIdAndStatusIgnoreCase(Long userId, String status);
}