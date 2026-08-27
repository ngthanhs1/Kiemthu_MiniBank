package com.minibank.backend.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.dto.TransferQrIntentResponse;
import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.entity.TransferQrIntent;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.account.repository.TransferQrIntentRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class TransferQrIntentService {
	private static final Duration TTL = Duration.ofMinutes(15);

	private final AccountRepository accountRepository;
	private final TransferQrIntentRepository intentRepository;
	private final UserRepository userRepository;

	public TransferQrIntentService(
		AccountRepository accountRepository,
		TransferQrIntentRepository intentRepository,
		UserRepository userRepository
	) {
		this.accountRepository = accountRepository;
		this.intentRepository = intentRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public TransferQrIntentResponse create(long userId, String accountNumber, BigDecimal amount) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		Account account = resolveOwnedActiveAccount(userId, accountNumber);
		BigDecimal normalizedAmount = normalizeAmount(amount);

		String intentToken = UUID.randomUUID().toString().replace("-", "");
		Instant expiresAt = Instant.now().plus(TTL);
		String payload = buildPayload(intentToken, account, normalizedAmount, expiresAt);

		TransferQrIntent intent = TransferQrIntent.builder()
			.intentToken(intentToken)
			.account(account)
			.amount(normalizedAmount)
			.status("active")
			.createdByUser(user)
			.expiresAt(expiresAt)
			.payload(payload)
			.build();
	
		intentRepository.save(intent);
		return toResponse(intent);
	}

	@Transactional(readOnly = true)
	public TransferQrIntentResponse getForOwner(long userId, long intentId) {
		TransferQrIntent intent = intentRepository.findByIdAndCreatedByUserId(intentId, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR intent not found"));
		return toResponse(ensureFresh(intent));
	}

	@Transactional
	public TransferQrIntentResponse claim(long userId, String intentToken) {
		TransferQrIntent intent = intentRepository.findByIntentToken(intentToken)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR intent not found"));
		ensureFresh(intent);

		if ("completed".equalsIgnoreCase(intent.getStatus())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "QR is already completed");
		}
		if ("claimed".equalsIgnoreCase(intent.getStatus()) && intent.getClaimedByUserId() != null && !intent.getClaimedByUserId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "QR has already been claimed");
		}

		intent.setStatus("claimed");
		intent.setClaimedByUserId(userId);
		intent.setClaimedAt(Instant.now());
		intentRepository.save(intent);
		return toResponse(intent);
	}

	@Transactional
	public void markCompleted(long intentId, long transactionId) {
		TransferQrIntent intent = intentRepository.findById(intentId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR intent not found"));
		if ("completed".equalsIgnoreCase(intent.getStatus())) {
			return;
		}
		intent.setStatus("completed");
		intent.setCompletedAt(Instant.now());
		intent.setCompletedTransactionId(transactionId);
		intentRepository.save(intent);
	}

	private TransferQrIntent ensureFresh(TransferQrIntent intent) {
		if (!"completed".equalsIgnoreCase(intent.getStatus()) && intent.getExpiresAt() != null && Instant.now().isAfter(intent.getExpiresAt())) {
			intent.setStatus("expired");
			intentRepository.save(intent);
		}
		return intent;
	}

	private Account resolveOwnedActiveAccount(long userId, String accountNumber) {
		String normalized = accountNumber == null ? "" : accountNumber.trim();
		Account account = accountRepository.findByAccountNumber(normalized)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
		if (account.getUser() == null || account.getUser().getId() == null || !account.getUser().getId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account does not belong to user");
		}
		if (!"active".equalsIgnoreCase(account.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account is not active");
		}
		return account;
	}

	private static BigDecimal normalizeAmount(BigDecimal amount) {
		if (amount == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount is required");
		}
		BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
		if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
		}
		return normalized;
	}

private static String buildPayload(String intentToken, Account account, BigDecimal amount, Instant expiresAt) {
    String safeName = account.getAccountName() == null ? "" : account.getAccountName().replace("\"", "\\\"");
    String expiresAtStr = expiresAt == null ? "" : expiresAt.toString();
    return "{"
        + "\"type\":\"transfer_request\","
        + "\"intentToken\":\"" + intentToken + "\","
        + "\"accountNumber\":\"" + account.getAccountNumber() + "\","
        + "\"accountName\":\"" + safeName + "\","
        + "\"amount\":\"" + amount.toPlainString() + "\","
        + "\"expiresAt\":\"" + expiresAtStr + "\""
        + "}";
}

	private TransferQrIntentResponse toResponse(TransferQrIntent intent) {
		return new TransferQrIntentResponse(
			intent.getId(),
			intent.getIntentToken(),
			intent.getAccount().getAccountNumber(),
			intent.getAccount().getAccountName(),
			intent.getAmount(),
			intent.getStatus(),
			intent.getPayload(),
			intent.getExpiresAt(),
			intent.getClaimedAt(),
			intent.getCompletedAt()
		);
	}

	@Transactional(readOnly = true)
	public TransferQrIntentResponse getLatestForOwner(long userId, String accountNumber) {
		Account account = resolveOwnedActiveAccount(userId, accountNumber);
		TransferQrIntent intent = intentRepository.findFirstByAccountIdOrderByCreatedAtDesc(account.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR intent not found"));
		return toResponse(ensureFresh(intent));
	}
}