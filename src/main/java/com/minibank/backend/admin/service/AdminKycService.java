package com.minibank.backend.admin.service;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.user.entity.KycRequest;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.KycRequestRepository;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class AdminKycService {
	private final KycRequestRepository kycRequestRepository;
	private final UserRepository userRepository;

	public AdminKycService(
		KycRequestRepository kycRequestRepository,
		UserRepository userRepository
	) {
		this.kycRequestRepository = kycRequestRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public void approve(Long kycRequestId, Long adminUserId, String note) {
		KycRequest kyc = kycRequestRepository.findById(kycRequestId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC request not found"));
		if (!"pending".equalsIgnoreCase(kyc.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KYC request is not pending");
		}

		User user = kyc.getUser();
		if (user == null || user.getId() == null) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "KYC request missing user");
		}

		// Update user profile from KYC data.
		user.setFullName(kyc.getFullName());
		user.setDob(kyc.getDob());
		user.setCitizenId(kyc.getCitizenId());
		user.setAddress(kyc.getAddress());
		user.setStatus("active");
		userRepository.save(user);

		kyc.setStatus("approved");
		kyc.setReviewedAt(Instant.now());
		kyc.setReviewNote(note);
		// reviewedBy link omitted in MVP (adminUser entity lookup not mandatory)
		kycRequestRepository.save(kyc);
	}

	@Transactional
	public void reject(Long kycRequestId, Long adminUserId, String note) {
		KycRequest kyc = kycRequestRepository.findById(kycRequestId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "KYC request not found"));
		if (!"pending".equalsIgnoreCase(kyc.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KYC request is not pending");
		}

		kyc.setStatus("rejected");
		kyc.setReviewedAt(Instant.now());
		kyc.setReviewNote(note);
		kycRequestRepository.save(kyc);
	}
}
