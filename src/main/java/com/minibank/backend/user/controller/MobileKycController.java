package com.minibank.backend.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.common.email.ResendEmailService;
import com.minibank.backend.common.otp.SmsOtpService;
import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.user.dto.KycOtpSendResponse;
import com.minibank.backend.user.dto.KycSubmitRequest;
import com.minibank.backend.user.entity.KycRequest;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.KycRequestRepository;
import com.minibank.backend.user.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/kyc")
public class MobileKycController {
	private final UserRepository userRepository;
	private final KycRequestRepository kycRequestRepository;
	private final SmsOtpService smsOtpService;
	private final ResendEmailService resendEmailService;

	public MobileKycController(
		UserRepository userRepository,
		KycRequestRepository kycRequestRepository,
		SmsOtpService smsOtpService,
		ResendEmailService resendEmailService
	) {
		this.userRepository = userRepository;
		this.kycRequestRepository = kycRequestRepository;
		this.smsOtpService = smsOtpService;
		this.resendEmailService = resendEmailService;
	}

	@PostMapping("/otp/send")
	@Transactional(readOnly = true)
	public KycOtpSendResponse sendOtp() {
		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		if (user.getPhone() == null || user.getPhone().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
		}
		if ("active".equalsIgnoreCase(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "KYC is already approved");
		}
		if (kycRequestRepository.existsByUserIdAndStatusIn(userId, java.util.List.of("pending", "approved"))) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "KYC request already exists");
		}

		SmsOtpService.OtpSendResult result = smsOtpService.sendOtp(user.getPhone());
		return new KycOtpSendResponse(result.devMode(), result.otp());
	}

	@PostMapping("/submit")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public void submit(@Valid @RequestBody KycSubmitRequest request) {
		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		if (user.getPhone() == null || user.getPhone().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is required");
		}
		if ("active".equalsIgnoreCase(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "KYC is already approved");
		}

		boolean otpOk = smsOtpService.verifyOtp(user.getPhone(), request.otpCode());
		if (!otpOk) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
		}
		if (kycRequestRepository.existsByUserIdAndStatusIn(userId, java.util.List.of("pending", "approved"))) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "KYC request already exists");
		}

		KycRequest kyc = KycRequest.builder()
			.user(user)
			.fullName(request.fullName().trim())
			.dob(request.dob())
			.citizenId(request.citizenId().trim())
			.address(request.address().trim())
			.occupation(request.occupation().trim())
			.monthlyIncome(request.monthlyIncome())
			.citizenFrontImageUrl(request.citizenFrontImageUrl().trim())
			.citizenBackImageUrl(request.citizenBackImageUrl().trim())
			.portraitImageUrl(request.portraitImageUrl().trim())
			.status("pending")
			.build();
		kycRequestRepository.save(kyc);

		if (user.getStatus() == null || user.getStatus().isBlank()) {
			user.setStatus("pending");
			userRepository.save(user);
		}

		resendEmailService.sendKycSubmittedEmail(user.getEmail(), request.fullName());
	}
}
