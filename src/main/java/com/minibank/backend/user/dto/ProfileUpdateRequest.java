package com.minibank.backend.user.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
	@Size(max = 255) String fullName,
	LocalDate dob,
	@Size(max = 2000) String address
) {}
