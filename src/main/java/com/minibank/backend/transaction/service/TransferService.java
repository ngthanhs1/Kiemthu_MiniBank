package com.minibank.backend.transaction.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.entity.AccountBalanceLedger;
import com.minibank.backend.account.entity.TransferQrIntent;
import com.minibank.backend.account.repository.AccountBalanceLedgerRepository;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.account.repository.TransferQrIntentRepository;
import com.minibank.backend.ai.service.AiTransactionClassificationService;
import com.minibank.backend.common.otp.SmsOtpService;
import com.minibank.backend.system.service.NotificationService;
import com.minibank.backend.transaction.dto.TransferConfirmRequest;
import com.minibank.backend.transaction.dto.TransferConfirmResponse;
import com.minibank.backend.transaction.dto.TransferInitiateRequest;
import com.minibank.backend.transaction.dto.TransferInitiateResponse;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.entity.TransactionAuthentication;
import com.minibank.backend.transaction.repository.TransactionAuthenticationRepository;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class TransferService {
	private static final SecureRandom RNG = new SecureRandom();
	private static final Logger log = LoggerFactory.getLogger(TransferService.class);

	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final TransactionAuthenticationRepository transactionAuthenticationRepository;
	private final AccountBalanceLedgerRepository ledgerRepository;
	private final TransferQrIntentRepository transferQrIntentRepository;
	private final PasswordEncoder passwordEncoder;
	private final RsaSignatureService rsaSignatureService;
	private final SmsOtpService smsOtpService;
	private final AiTransactionClassificationService aiTransactionClassificationService;
	private final NotificationService notificationService;
	private final boolean debugReturnOtp;
	private final BigDecimal largeThreshold;
	private final BigDecimal managerThreshold;

	public TransferService(
		UserRepository userRepository,
		AccountRepository accountRepository,
		TransactionRepository transactionRepository,
		TransactionAuthenticationRepository transactionAuthenticationRepository,
		AccountBalanceLedgerRepository ledgerRepository,
		TransferQrIntentRepository transferQrIntentRepository,
		PasswordEncoder passwordEncoder,
		RsaSignatureService rsaSignatureService,
		SmsOtpService smsOtpService,
		AiTransactionClassificationService aiTransactionClassificationService,
		NotificationService notificationService,
		@Value("${app.otp.debug-return:false}") boolean debugReturnOtp,
		@Value("${app.transaction.large-threshold:100000000}") BigDecimal largeThreshold,
		@Value("${app.transaction.manager-threshold:200000000}") BigDecimal managerThreshold
	) {
		this.userRepository = userRepository;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.transactionAuthenticationRepository = transactionAuthenticationRepository;
		this.ledgerRepository = ledgerRepository;
		this.transferQrIntentRepository = transferQrIntentRepository;
		this.passwordEncoder = passwordEncoder;
		this.rsaSignatureService = rsaSignatureService;
		this.smsOtpService = smsOtpService;
		this.aiTransactionClassificationService = aiTransactionClassificationService;
		this.notificationService = notificationService;
		this.debugReturnOtp = debugReturnOtp;
		this.largeThreshold = largeThreshold == null ? BigDecimal.ZERO : largeThreshold;
		this.managerThreshold = managerThreshold == null ? BigDecimal.ZERO : managerThreshold;
	}

	@Transactional
	public TransferInitiateResponse initiate(long userId, TransferInitiateRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		if (!"active".equalsIgnoreCase(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
		}

		Account fromAccount = resolveFromAccount(userId, request.fromAccountNumber());
		Account toAccount = accountRepository.findByAccountNumber(request.toAccountNumber().trim())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Recipient account not found"));
		if (!"active".equalsIgnoreCase(toAccount.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recipient account is not active");
		}


		BigDecimal amount = normalizeAmount(request.amount());
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
		}

		if (fromAccount.getAvailableBalance().compareTo(amount) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
		}

		BigDecimal dailyLimit = fromAccount.getDailyTransferLimit() == null ? BigDecimal.ZERO : fromAccount.getDailyTransferLimit();
		if (dailyLimit.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(dailyLimit) > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount exceeds daily transfer limit");
		}

		BigDecimal receiveLimit = toAccount.getDailyReceiveLimit() == null ? BigDecimal.ZERO : toAccount.getDailyReceiveLimit();
		if (receiveLimit.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(receiveLimit) > 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount exceeds recipient daily receive limit");
		}

		String canonical = canonicalPayload(fromAccount.getAccountNumber(), toAccount.getAccountNumber(), amount, request.description(), request.idempotencyKey());
		rsaSignatureService.verifyOrThrow(user.getPublicKey(), canonical, request.signature());
		
		if (user.getTransactionPinHash() == null || user.getTransactionPinHash().isBlank()) {
			throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "Transaction PIN is not set");
		}
		if (!passwordEncoder.matches(request.pin(), user.getTransactionPinHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PIN");
		}

		Transaction tx = null;
		TransferQrIntent qrIntent = null;
		if (request.qrTransferIntentId() != null) {
			qrIntent = transferQrIntentRepository.findById(request.qrTransferIntentId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "QR transfer intent not found"));
			if (qrIntent.getCreatedByUser() == null || qrIntent.getCreatedByUser().getId() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR transfer intent is invalid");
			}
			if (!qrIntent.getCreatedByUser().getId().equals(user.getId()) && !user.getId().equals(qrIntent.getClaimedByUserId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "QR transfer intent is not claimed by this user");
			}
			if (!"claimed".equalsIgnoreCase(qrIntent.getStatus()) && !"active".equalsIgnoreCase(qrIntent.getStatus())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "QR transfer intent is not available");
			}
		}

		if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
			Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey().trim());
			if (existing.isPresent()) {
				tx = existing.get();
				if (tx.getInitiatedByUser() == null || tx.getInitiatedByUser().getId() == null || !tx.getInitiatedByUser().getId().equals(user.getId())) {
					throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key already used");
				}
			}
		}

		SmsOtpService.OtpSendResult otpResult = smsOtpService.sendOtp(user.getPhone());
		String otp = otpResult.otp();
		String otpHash = otp == null ? null : sha256Hex(otp);

		if (tx == null) {
			tx = Transaction.builder()
				.transactionCode(generateTransactionCode())
				.idempotencyKey(request.idempotencyKey() == null ? null : request.idempotencyKey().trim())
				.fromAccount(fromAccount)
				.toAccount(toAccount)
				.transactionType("transfer")
				.amount(amount)
				.feeAmount(BigDecimal.ZERO)
				.description(request.description() == null ? null : request.description().trim())
				.status("pending")
				.initiatedByUser(user)
					.qrTransferIntent(qrIntent)
				.build();
			tx = transactionRepository.save(tx);

			TransactionAuthentication auth = TransactionAuthentication.builder()
				.transaction(tx)
				.pinVerified(true)
				.otpVerified(false)
				.otpCodeHash(otpHash)
				.digitalSignature(request.signature())
				.authStatus("pin_verified")
				.build();
			transactionAuthenticationRepository.save(auth);
		} else {
			if (!"pending".equalsIgnoreCase(tx.getStatus())) {
				return new TransferInitiateResponse(
					tx.getId(),
					tx.getTransactionCode(),
					tx.getStatus(),
					fromAccount.getAccountNumber(),
					toAccount.getAccountNumber(),
					toAccount.getAccountName(),
					tx.getAmount(),
					true,
					true,
					null
				);
			}

			TransactionAuthentication auth = transactionAuthenticationRepository.findByTransactionId(tx.getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing transaction authentication"));
			auth.setOtpCodeHash(otpHash);
			auth.setOtpVerified(false);
			auth.setPinVerified(true);
			auth.setDigitalSignature(request.signature());
			auth.setAuthStatus("pin_verified");
			transactionAuthenticationRepository.save(auth);
		}

		if (qrIntent != null) {
			if (tx.getQrTransferIntent() == null) {
				tx.setQrTransferIntent(qrIntent);
				transactionRepository.save(tx);
			}
		}

		return new TransferInitiateResponse(
			tx.getId(),
			tx.getTransactionCode(),
			tx.getStatus(),
			fromAccount.getAccountNumber(),
			toAccount.getAccountNumber(),
			toAccount.getAccountName(),
			amount,
			true,
			true,
			debugReturnOtp ? otp : null
		);
	}

	private static String sha256Hex(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Transactional
	public TransferConfirmResponse confirm(long userId, TransferConfirmRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		if (!"active".equalsIgnoreCase(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is not active");
		}

		Transaction tx = transactionRepository.findByIdAndInitiatedByUserId(request.transactionId(), user.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
		if ("completed".equalsIgnoreCase(tx.getStatus())) {
			Account fromAcc = tx.getFromAccount();
			return new TransferConfirmResponse(tx.getId(), tx.getStatus(), tx.getCompletedAt(), fromAcc.getAccountNumber(), tx.getToAccount().getAccountNumber(), tx.getAmount(), fromAcc.getAvailableBalance());
		}
		if (!"pending".equalsIgnoreCase(tx.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction is not pending");
		}

		TransactionAuthentication auth = transactionAuthenticationRepository.findByTransactionId(tx.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing transaction authentication"));

		if (auth.getOtpCodeHash() == null || !sha256Hex(request.otpCode()).equals(auth.getOtpCodeHash())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP");
		}
		auth.setOtpVerified(true);
		auth.setAuthStatus("verified");
		auth.setVerifiedAt(Instant.now());
		transactionAuthenticationRepository.save(auth);

		if (requiresLargeApproval(tx.getAmount())) {
			String nextStatus = requiresManagerApproval(tx.getAmount()) ? "pending_manager" : "pending_review";
			tx.setStatus(nextStatus);
			transactionRepository.save(tx);
			Account fromAcc = tx.getFromAccount();
			return new TransferConfirmResponse(
				tx.getId(),
				tx.getStatus(),
				tx.getCompletedAt(),
				fromAcc.getAccountNumber(),
				tx.getToAccount().getAccountNumber(),
				tx.getAmount(),
				fromAcc.getAvailableBalance()
			);
		}
	
		// ACID transfer: lock both accounts before updating balances.
		Account fromAccount = accountRepository.findByIdForUpdate(tx.getFromAccount().getId())
		.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Sender account missing"));
		Account toAccount = accountRepository.findByIdForUpdate(tx.getToAccount().getId())
		.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Recipient account missing"));
		
		BigDecimal amount = tx.getAmount();
		if (fromAccount.getAvailableBalance().compareTo(amount) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
		}

		BigDecimal fromBefore = fromAccount.getAvailableBalance();
		BigDecimal toBefore = toAccount.getAvailableBalance();
		
		fromAccount.setAvailableBalance(fromBefore.subtract(amount));
		fromAccount.setCurrentBalance(fromAccount.getCurrentBalance().subtract(amount));
		toAccount.setAvailableBalance(toBefore.add(amount));
		toAccount.setCurrentBalance(toAccount.getCurrentBalance().add(amount));

		accountRepository.save(fromAccount);
		accountRepository.save(toAccount);
		
		ledgerRepository.save(AccountBalanceLedger.builder()
			.account(fromAccount)
			.transaction(tx)
			.entryType("debit")
			.amount(amount)
			.balanceBefore(fromBefore)
			.balanceAfter(fromAccount.getAvailableBalance())
			.build());
			
			ledgerRepository.save(AccountBalanceLedger.builder()
			.account(toAccount)
			.transaction(tx)
			.entryType("credit")
			.amount(amount)
			.balanceBefore(toBefore)
			.balanceAfter(toAccount.getAvailableBalance())
			.build());

			tx.setStatus("completed");
			tx.setCompletedAt(Instant.now());
		transactionRepository.save(tx);
		if (tx.getQrTransferIntent() != null) {
			transferQrIntentRepository.findById(tx.getQrTransferIntent().getId())
				.ifPresent(intent -> transferQrIntentRepository.save(markCompleted(intent, tx.getId())));
		}

		try {
			aiTransactionClassificationService.classifyIfEligible(tx);
		} catch (Exception ex) {
			log.warn("Failed to classify transaction {}: {}", tx.getId(), ex.getMessage());
		}
		notifyTransferCompleted(tx, fromAccount, toAccount);
	
		return new TransferConfirmResponse(
			tx.getId(),
			tx.getStatus(),
			tx.getCompletedAt(),
			fromAccount.getAccountNumber(),
			toAccount.getAccountNumber(),
			amount,
			fromAccount.getAvailableBalance()
		);
	}

	private static TransferQrIntent markCompleted(TransferQrIntent intent, long transactionId) {
		intent.setStatus("completed");
		intent.setCompletedAt(Instant.now());
		intent.setCompletedTransactionId(transactionId);
		return intent;
	}

	private boolean requiresLargeApproval(BigDecimal amount) {
		return largeThreshold.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(largeThreshold) >= 0;
	}

	private boolean requiresManagerApproval(BigDecimal amount) {
		return managerThreshold.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(managerThreshold) >= 0;
	}

	private void notifyTransferCompleted(Transaction tx, Account fromAccount, Account toAccount) {
		String amountText = tx.getAmount().setScale(0, RoundingMode.HALF_UP).toPlainString() + " VND";
		if (fromAccount.getUser() != null && fromAccount.getUser().getId() != null) {
			notificationService.createForUser(
				fromAccount.getUser().getId(),
				"TRANSFER",
				"Chuyen khoan thanh cong",
				"Ban da chuyen " + amountText + " den " + toAccount.getAccountNumber()
			);
		}
		if (toAccount.getUser() != null && toAccount.getUser().getId() != null) {
			notificationService.createForUser(
				toAccount.getUser().getId(),
				"TRANSFER",
				"Ban vua nhan tien",
				"Tai khoan " + toAccount.getAccountNumber() + " nhan " + amountText + " tu " + fromAccount.getAccountNumber()
			);
		}
	}

	private Account resolveFromAccount(long userId, String fromAccountNumber) {
		if (fromAccountNumber != null && !fromAccountNumber.isBlank()) {
			Account acc = accountRepository.findByAccountNumber(fromAccountNumber.trim())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sender account not found"));
			if (acc.getUser() == null || acc.getUser().getId() == null || acc.getUser().getId() != userId) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Sender account does not belong to user");
			}
			return acc;
		}

		return accountRepository.findByUserIdOrderByIdAsc(userId).stream()
			.findFirst()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "No account is assigned"));
	}

	private static BigDecimal normalizeAmount(BigDecimal amount) {
		if (amount == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount is required");
		}
		return amount.setScale(2, RoundingMode.HALF_UP);
	}

	private static String generateTransactionCode() {
		long now = System.currentTimeMillis();
		int r = RNG.nextInt(900_000) + 100_000;
		return "TX" + now + r;
	}

	private static String canonicalPayload(String fromAcc, String toAcc, BigDecimal amount, String description, String idempotencyKey) {
		String desc = description == null ? "" : description.trim();
		String idem = idempotencyKey == null ? "" : idempotencyKey.trim();
		String amt = amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
		return "from=" + fromAcc + "|to=" + toAcc + "|amount=" + amt + "|description=" + desc + "|idempotencyKey=" + idem;
	}
}
