package com.minibank.backend.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTransferQrRequest(
	@NotBlank String accountNumber,
	@NotNull BigDecimal amount
) {}