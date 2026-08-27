package com.minibank.backend.system.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.system.entity.SystemLog;
import com.minibank.backend.system.repository.SystemLogRepository;

@RestController
@RequestMapping("/api/system-logs")
public class SystemLogController {

    private final SystemLogRepository systemLogRepository;

    public SystemLogController(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    @GetMapping
    public List<SystemLog> getLogs() {
        return systemLogRepository.findAll();
    }
}