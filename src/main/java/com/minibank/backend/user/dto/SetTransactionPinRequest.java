package com.minibank.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetTransactionPinRequest(
	@Size(min = 6, max = 6) String oldPin,
	@NotBlank @Size(min = 6, max = 6) String newPin
) {}
