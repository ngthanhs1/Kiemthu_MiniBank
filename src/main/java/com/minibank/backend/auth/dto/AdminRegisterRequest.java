package com.minibank.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRegisterRequest(
	@NotBlank @Size(min = 3, max = 100) String username,
	@NotBlank @Email String email,
	@NotBlank @Size(min = 6, max = 100) String password,
	@NotBlank @Size(max = 255) String fullName
) {}
