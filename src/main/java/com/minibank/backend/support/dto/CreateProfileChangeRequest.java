package com.minibank.backend.support.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

public record CreateProfileChangeRequest(
	@Size(max = 255) String fullName,
	LocalDate dob,
	@Size(max = 2000) String address,
	@Size(max = 1000) String reason
) {}
