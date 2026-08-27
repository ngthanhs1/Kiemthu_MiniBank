package com.minibank.backend.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.auth.dto.AuthOtpSendResponse;
import com.minibank.backend.auth.dto.AuthResponse;
import com.minibank.backend.auth.dto.LoginOtpSendRequest;
import com.minibank.backend.auth.dto.LoginRequest;
import com.minibank.backend.auth.dto.LoginVerifyRequest;
import com.minibank.backend.auth.dto.PasswordResetOtpSendRequest;
import com.minibank.backend.auth.dto.PasswordResetVerifyRequest;
import com.minibank.backend.auth.dto.PinResetVerifyRequest;
import com.minibank.backend.auth.dto.PinVerifyRequest;
import com.minibank.backend.auth.dto.UserRegisterRequest;
import com.minibank.backend.auth.service.AuthService;
import com.minibank.backend.common.security.CurrentJwt;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/auth")
public class MobileAuthController {
	private static final Logger log = LoggerFactory.getLogger(MobileAuthController.class);

	private final AuthService authService;

	public MobileAuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthResponse register(@Valid @RequestBody UserRegisterRequest request) {
		log.info("/api/mobile/auth/register phone={}, email={}", request.phone(), request.email());
		return authService.registerUser(request);
	}

	@PostMapping("/login")
	public AuthResponse login(@Valid @RequestBody LoginRequest request) {
		log.info("/api/mobile/auth/login identifier={}, deviceIdPresent={}", request.identifier(), request.deviceId() != null && !request.deviceId().isBlank());
		return authService.loginUser(request);
	}

	@PostMapping("/login/otp/send")
	public AuthOtpSendResponse sendOtp(@Valid @RequestBody LoginOtpSendRequest request) {
		log.info("/api/mobile/auth/login/otp/send identifier={}", request.identifier());
		return authService.sendLoginOtp(request);
	}

	@PostMapping("/login/verify")
	public AuthResponse verify(@Valid @RequestBody LoginVerifyRequest request) {
		log.info("/api/mobile/auth/login/verify identifier={}", request.identifier());
		return authService.verifyLogin(request);
	}

	@PostMapping("/pin/verify")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void verifyPin(@Valid @RequestBody PinVerifyRequest request) {
		long userId = CurrentJwt.requireUserId();
		authService.verifyPin(userId, request.pin());
	}

	@PostMapping("/password/reset/otp/send")
	public AuthOtpSendResponse sendPasswordResetOtp(@Valid @RequestBody PasswordResetOtpSendRequest request) {
		log.info("/api/mobile/auth/password/reset/otp/send identifier={}", request.identifier());
		return authService.sendPasswordResetOtp(request);
	}

	@PostMapping("/password/reset/verify")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetPassword(@Valid @RequestBody PasswordResetVerifyRequest request) {
		log.info("/api/mobile/auth/password/reset/verify identifier={}", request.identifier());
		authService.resetPassword(request);
	}

	@PostMapping("/pin/reset/otp/send")
	public AuthOtpSendResponse sendPinResetOtp() {
		long userId = CurrentJwt.requireUserId();
		log.info("/api/mobile/auth/pin/reset/otp/send userId={}", userId);
		return authService.sendPinResetOtp(userId);
	}

	@PostMapping("/pin/reset/verify")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetPin(@Valid @RequestBody PinResetVerifyRequest request) {
		long userId = CurrentJwt.requireUserId();
		log.info("/api/mobile/auth/pin/reset/verify userId={}", userId);
		authService.resetPin(userId, request);
	}
}
