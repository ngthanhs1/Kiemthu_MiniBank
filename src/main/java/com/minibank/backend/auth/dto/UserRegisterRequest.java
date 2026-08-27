package com.minibank.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
	@NotBlank @Size(min = 8, max = 20) String phone,
	@NotBlank @Email String email,
	@NotBlank @Size(min = 6, max = 100) String password,
	@Size(max = 255) String fullName,
	@Size(min = 6, max = 128) String deviceId,
	String publicKey
) {}
