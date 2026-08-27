package com.minibank.backend.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KycSubmitRequest(
	@NotBlank @Size(max = 255) String fullName,
	@NotNull LocalDate dob,
	@NotBlank @Size(max = 50) String citizenId,
	@NotBlank @Size(max = 2000) String address,
	@NotBlank @Size(max = 255) String occupation,
	@NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal monthlyIncome,
	@NotBlank @Size(max = 4000) String citizenFrontImageUrl,
	@NotBlank @Size(max = 4000) String citizenBackImageUrl,
	@NotBlank @Size(max = 4000) String portraitImageUrl,
	@NotBlank @Size(min = 6, max = 6) String otpCode
) {}
