package com.minibank.backend.system.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minibank.backend.system.entity.SystemLog;
import com.minibank.backend.system.repository.SystemLogRepository;

@Service
public class SystemLogService {

	private final SystemLogRepository systemLogRepository;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public SystemLogService(SystemLogRepository systemLogRepository) {
		this.systemLogRepository = systemLogRepository;
	}

	@Transactional
	public void log(SystemLog entry) {
		systemLogRepository.save(entry);
	}

	public String toJson(Map<String, Object> metadata) {
		if (metadata == null || metadata.isEmpty()) {
			return null;
		}

		try {
			return objectMapper.writeValueAsString(metadata);
		} catch (Exception e) {
			return null;
		}
	}

	public List<SystemLog> getAllLogs() {
		return systemLogRepository.findAll();
	}
}