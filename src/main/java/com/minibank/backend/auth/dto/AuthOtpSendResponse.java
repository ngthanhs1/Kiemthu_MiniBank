package com.minibank.backend.auth.dto;

public record AuthOtpSendResponse(
	boolean devMode,
	String otp
) {}
