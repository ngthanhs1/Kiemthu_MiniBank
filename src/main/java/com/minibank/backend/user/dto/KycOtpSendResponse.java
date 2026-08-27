package com.minibank.backend.user.dto;

public record KycOtpSendResponse(
	boolean devMode,
	String otp
) {}
