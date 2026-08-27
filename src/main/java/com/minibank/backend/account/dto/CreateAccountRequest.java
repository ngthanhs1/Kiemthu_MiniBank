package com.minibank.backend.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
	@NotBlank @Pattern(regexp = "^[0-9]{13}$") String accountNumber
) {}
