package com.minibank.backend.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.user.dto.ChangePasswordRequest;
import com.minibank.backend.user.dto.DocumentUploadRequest;
import com.minibank.backend.user.dto.ProfileResponse;
import com.minibank.backend.user.dto.ProfileUpdateRequest;
import com.minibank.backend.user.dto.PublicKeyRequest;
import com.minibank.backend.user.dto.SetTransactionPinRequest;
import com.minibank.backend.user.entity.Document;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.DocumentRepository;
import com.minibank.backend.user.repository.UserRepository;
import com.minibank.backend.user.service.CustomerRankService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/profile")
public class MobileProfileController {
	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final PasswordEncoder passwordEncoder;
	private final DocumentRepository documentRepository;
	private final CustomerRankService customerRankService;

	public MobileProfileController(UserRepository userRepository, AccountRepository accountRepository, PasswordEncoder passwordEncoder, DocumentRepository documentRepository, CustomerRankService customerRankService) {
		this.userRepository = userRepository;
		this.accountRepository = accountRepository;
		this.passwordEncoder = passwordEncoder;
		this.documentRepository = documentRepository;
		this.customerRankService = customerRankService;
	}

	@GetMapping("/me")
	@Transactional
	public ProfileResponse me() {
		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		List<ProfileResponse.AccountSummary> accounts = accountRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
			.map(a -> new ProfileResponse.AccountSummary(a.getId(), a.getAccountNumber(), a.getAccountName(), a.getStatus()))
			.toList();

		String customerRank = customerRankService.refreshRank(user);
		return new ProfileResponse(
			user.getId(),
			user.getPhone(),
			user.getEmail(),
			user.getFullName(),
			user.getDob(),
			user.getAddress(),
			user.getStatus(),
			customerRank,
			user.getTransactionPinHash() != null && !user.getTransactionPinHash().isBlank(),
			user.getPublicKey() != null && !user.getPublicKey().isBlank(),
			user.getDeviceId(),
			accounts
		);
	}

	@PutMapping("/me")
	@Transactional
	public ProfileResponse update(@Valid @RequestBody ProfileUpdateRequest request) {
		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		if (request.fullName() != null) {
			user.setFullName(request.fullName().trim());
		}
		if (request.dob() != null) {
			user.setDob(request.dob());
		}
		if (request.address() != null) {
			user.setAddress(request.address().trim());
		}

		userRepository.save(user);
		return me();
	}

	@PostMapping("/change-password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void changePassword(@Valid @RequestBody ChangePasswordRequest request) {
		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid old password");
		}
		user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);
	}

	@PostMapping("/transaction-pin")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void setOrChangePin(@Valid @RequestBody SetTransactionPinRequest request) {
		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		if (!request.newPin().matches("^[0-9]{6}$")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PIN must be 6 digits");
		}

		boolean hasPin = user.getTransactionPinHash() != null && !user.getTransactionPinHash().isBlank();
		if (hasPin) {
			if (request.oldPin() == null || request.oldPin().isBlank()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "oldPin is required");
			}
			if (!passwordEncoder.matches(request.oldPin(), user.getTransactionPinHash())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid old PIN");
			}
		}

		user.setTransactionPinHash(passwordEncoder.encode(request.newPin()));
		userRepository.save(user);
	}

	@PostMapping("/public-key")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void setPublicKey(@Valid @RequestBody PublicKeyRequest request) {
		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		String pem = request.publicKey().trim();
		if (!pem.contains("BEGIN PUBLIC KEY")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "publicKey must be PEM encoded");
		}

		user.setPublicKey(pem);
		userRepository.save(user);
	}

	@PostMapping("/reset-device")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void resetDevice() {
		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		user.setDeviceId(null);
		userRepository.save(user);
	}

	@PostMapping("/documents")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public Document uploadDocument(@Valid @RequestBody DocumentUploadRequest request) {
		long userId = CurrentJwt.requireUserId();
		Document doc = Document.builder()
			.ownerType("USER")
			.ownerId(userId)
			.documentType(request.documentType().trim())
			.fileName(blankToNull(request.fileName()))
			.fileUrl(request.fileUrl().trim())
			.mimeType(blankToNull(request.mimeType()))
			.verifiedStatus("pending")
			.signedStatus(isContractType(request.documentType()) ? "pending" : "not_applicable")
			.uploadedByType("USER")
			.uploadedById(userId)
			.note(blankToNull(request.note()))
			.build();
		return documentRepository.save(doc);
	}

	@PostMapping("/documents/{documentId}/sign")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void signDocument(@PathVariable("documentId") Long documentId) {
		long userId = CurrentJwt.requireUserId();
		Document doc = documentRepository.findById(documentId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
		if (!"USER".equals(doc.getOwnerType()) || !userIdEquals(doc.getOwnerId(), userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to sign this document");
		}

		if (!"pending".equalsIgnoreCase(doc.getSignedStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Document is not pending signature");
		}

		doc.setSignedStatus("signed");
		doc.setSignedByUserId(userId);
		doc.setSignedAt(java.time.Instant.now());
		documentRepository.save(doc);
	}

	private static boolean userIdEquals(Long a, long b) {
		if (a == null) return false;
		return a.longValue() == b;
	}

	private static boolean isContractType(String documentType) {
		String normalized = documentType == null ? "" : documentType.trim().toLowerCase();
		return normalized.equals("loan_contract") || normalized.equals("saving_contract");
	}

	private static String blankToNull(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
