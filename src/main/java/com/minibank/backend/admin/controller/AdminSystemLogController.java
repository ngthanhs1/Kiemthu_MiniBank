package com.minibank.backend.admin.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.dto.SystemLogItem;
import com.minibank.backend.admin.dto.SystemLogPageResponse;
import com.minibank.backend.system.entity.SystemLog;
import com.minibank.backend.system.repository.SystemLogRepository;

@RestController
@RequestMapping("/api/admin/system/logs")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'Xem log hệ thống')")
public class AdminSystemLogController {
	private static final int MAX_PAGE_SIZE = 200;

	private final SystemLogRepository systemLogRepository;

	public AdminSystemLogController(SystemLogRepository systemLogRepository) {
		this.systemLogRepository = systemLogRepository;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public SystemLogPageResponse list(
			@RequestParam(value = "actorType", required = false) String actorType,
			@RequestParam(value = "actorId", required = false) Long actorId,
			@RequestParam(value = "action", required = false) String action,
			@RequestParam(value = "targetType", required = false) String targetType,
			@RequestParam(value = "targetId", required = false) Long targetId,
			@RequestParam(value = "from", required = false) String from,
			@RequestParam(value = "to", required = false) String to,
			@RequestParam(value = "page", defaultValue = "0") int page,
			@RequestParam(value = "size", defaultValue = "50") int size) {
		Instant fromTime = parseInstant(from, "from");
		Instant toTime = parseInstant(to, "to");
		int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
		Page<SystemLog> result = systemLogRepository.findAll(
				PageRequest.of(Math.max(page, 0), safeSize));
		List<SystemLogItem> items = result.getContent().stream().map(this::toItem).toList();
		return new SystemLogPageResponse(items, result.getTotalElements(), Math.max(page, 0), safeSize);
	}

	private SystemLogItem toItem(SystemLog log) {
		return new SystemLogItem(
				log.getId(),
				log.getActorType(),
				log.getActorId(),
				log.getAction(),
				log.getTargetType(),
				log.getTargetId(),
				log.getMetadataJson(),
				log.getIpAddress(),
				log.getCreatedAt());
	}

	private static String normalize(String value) {
		if (value == null)
			return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed.toLowerCase();
	}

	private static Instant parseInstant(String value, String field) {
		if (value == null || value.isBlank())
			return null;
		try {
			return Instant.parse(value.trim());
		} catch (Exception e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + field + " timestamp");
		}
	}
}
