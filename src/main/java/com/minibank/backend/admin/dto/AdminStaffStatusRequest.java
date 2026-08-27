package com.minibank.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminStaffStatusRequest(@NotBlank String status) {}
