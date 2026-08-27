package com.minibank.backend.saving.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SavingOpenInitiateRequest(
	@NotNull(message = "Saving product ID is required")
	Long savingProductId,

	@NotNull(message = "Source account ID is required")
	Long sourceAccountId,

	Long settlementAccountId,

	Boolean autoRenew,

	@NotNull(message = "Principal amount is required")
	@Positive(message = "Principal amount must be positive")
	BigDecimal principalAmount,

	@NotNull(message = "agreementAccepted is required")
	Boolean agreementAccepted,

	String agreementVersion
) {
	@AssertTrue(message = "Bạn cần đọc và chấp nhận thỏa thuận trước khi nhận OTP")
	public boolean isAgreementAccepted() {
		return Boolean.TRUE.equals(agreementAccepted);
	}
}
