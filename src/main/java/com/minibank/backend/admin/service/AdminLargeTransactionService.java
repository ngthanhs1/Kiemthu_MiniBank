package com.minibank.backend.admin.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.entity.AccountBalanceLedger;
import com.minibank.backend.account.entity.TransferQrIntent;
import com.minibank.backend.account.repository.AccountBalanceLedgerRepository;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.account.repository.TransferQrIntentRepository;
import com.minibank.backend.admin.dto.LargeTransactionDetail;
import com.minibank.backend.admin.dto.LargeTransactionSummary;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.entity.TransactionAuthentication;
import com.minibank.backend.transaction.repository.TransactionAuthenticationRepository;
import com.minibank.backend.transaction.repository.TransactionRepository;

@Service
public class AdminLargeTransactionService {
	private final TransactionRepository transactionRepository;
	private final TransactionAuthenticationRepository transactionAuthenticationRepository;
	private final AccountRepository accountRepository;
	private final AccountBalanceLedgerRepository ledgerRepository;
	private final TransferQrIntentRepository transferQrIntentRepository;
	private final BigDecimal largeThreshold;
	private final BigDecimal managerThreshold;

	public AdminLargeTransactionService(
		TransactionRepository transactionRepository,
		TransactionAuthenticationRepository transactionAuthenticationRepository,
		AccountRepository accountRepository,
		AccountBalanceLedgerRepository ledgerRepository,
		TransferQrIntentRepository transferQrIntentRepository,
		@Value("${app.transaction.large-threshold:100000000}") BigDecimal largeThreshold,
		@Value("${app.transaction.manager-threshold:200000000}") BigDecimal managerThreshold
	) {
		this.transactionRepository = transactionRepository;
		this.transactionAuthenticationRepository = transactionAuthenticationRepository;
		this.accountRepository = accountRepository;
		this.ledgerRepository = ledgerRepository;
		this.transferQrIntentRepository = transferQrIntentRepository;
		this.largeThreshold = largeThreshold == null ? BigDecimal.ZERO : largeThreshold;
		this.managerThreshold = managerThreshold == null ? BigDecimal.ZERO : managerThreshold;
	}

	@Transactional(readOnly = true)
	public List<LargeTransactionSummary> list(String q, String status, String risk) {
		List<String> statuses = resolveStatuses(status);
		String normalizedQuery = normalize(q);
		List<Transaction> items = transactionRepository.findLargePending(statuses, largeThreshold);
		String normalizedRisk = normalize(risk);
		return items.stream()
			.filter(item -> matchesQuery(item, normalizedQuery))
			.map(this::toSummary)
			.filter(item -> normalizedRisk == null || normalizedRisk.equalsIgnoreCase(item.riskLevel()))
			.toList();
	}

	@Transactional(readOnly = true)
	public LargeTransactionDetail detail(Long transactionId) {
		Transaction tx = transactionRepository.findById(transactionId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
		return toDetail(tx);
	}

	@Transactional
	public void approve(Long transactionId, Long adminUserId, String note) {
		Transaction tx = transactionRepository.findById(transactionId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
		if (!isPendingReview(tx.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction is not awaiting approval");
		}
		TransactionAuthentication auth = transactionAuthenticationRepository.findByTransactionId(tx.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Missing transaction authentication"));
		if (!auth.isOtpVerified()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction OTP is not verified");
		}
		settleTransaction(tx);
	}

	@Transactional
	public void reject(Long transactionId, Long adminUserId, String note) {
		Transaction tx = transactionRepository.findById(transactionId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
		if (!isPendingReview(tx.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction is not awaiting approval");
		}
		tx.setStatus("rejected");
		tx.setCompletedAt(Instant.now());
		transactionRepository.save(tx);
	}

	private void settleTransaction(Transaction tx) {
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
	}

	private static TransferQrIntent markCompleted(TransferQrIntent intent, long transactionId) {
		intent.setStatus("completed");
		intent.setCompletedAt(Instant.now());
		intent.setCompletedTransactionId(transactionId);
		return intent;
	}

	private LargeTransactionSummary toSummary(Transaction tx) {
		String risk = resolveRisk(tx.getAmount());
		return new LargeTransactionSummary(
			tx.getId(),
			tx.getTransactionCode(),
			nullSafe(tx.getFromAccount().getUser().getFullName(), tx.getFromAccount().getAccountName()),
			tx.getFromAccount().getAccountNumber(),
			nullSafe(tx.getToAccount().getUser().getFullName(), tx.getToAccount().getAccountName()),
			tx.getToAccount().getAccountNumber(),
			tx.getAmount(),
			tx.getFromAccount().getCurrency(),
			risk,
			mapReviewStatus(tx.getStatus()),
			tx.getCreatedAt()
		);
	}

	private LargeTransactionDetail toDetail(Transaction tx) {
		String risk = resolveRisk(tx.getAmount());
		return new LargeTransactionDetail(
			tx.getId(),
			tx.getTransactionCode(),
			tx.getTransactionType(),
			nullSafe(tx.getFromAccount().getUser().getFullName(), tx.getFromAccount().getAccountName()),
			tx.getFromAccount().getAccountNumber(),
			nullSafe(tx.getToAccount().getUser().getFullName(), tx.getToAccount().getAccountName()),
			tx.getToAccount().getAccountNumber(),
			tx.getAmount(),
			tx.getFeeAmount(),
			tx.getDescription(),
			tx.getFromAccount().getCurrency(),
			risk,
			mapReviewStatus(tx.getStatus()),
			tx.getStatus(),
			tx.getCreatedAt(),
			tx.getCompletedAt()
		);
	}

	private String resolveRisk(BigDecimal amount) {
		if (managerThreshold.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(managerThreshold) >= 0) {
			return "high";
		}
		if (largeThreshold.compareTo(BigDecimal.ZERO) > 0 && amount.compareTo(largeThreshold) >= 0) {
			return "medium";
		}
		return "low";
	}

	private boolean isPendingReview(String status) {
		String normalized = normalize(status);
		return "pending_review".equals(normalized) || "pending_manager".equals(normalized);
	}

	private static String mapReviewStatus(String status) {
		String normalized = normalize(status);
		if (normalized == null) return "unknown";
		return normalized;
	}

	private static List<String> resolveStatuses(String status) {
		String normalized = normalize(status);
		if (normalized == null || "all".equals(normalized)) {
			return List.of("pending_review", "pending_manager");
		}
		return List.of(normalized);
	}

	private static String normalize(String value) {
		if (value == null) return null;
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed.toLowerCase();
	}

	private static String nullSafe(String primary, String fallback) {
		if (primary != null && !primary.isBlank()) return primary;
		return fallback == null ? "" : fallback;
	}

	private static boolean matchesQuery(Transaction tx, String query) {
		if (query == null) return true;
		String needle = query.toLowerCase();
		return containsIgnoreCase(tx.getTransactionCode(), needle)
			|| containsIgnoreCase(tx.getFromAccount().getAccountNumber(), needle)
			|| containsIgnoreCase(tx.getToAccount().getAccountNumber(), needle)
			|| containsIgnoreCase(tx.getFromAccount().getAccountName(), needle)
			|| containsIgnoreCase(tx.getToAccount().getAccountName(), needle);
	}

	private static boolean containsIgnoreCase(String value, String needleLower) {
		if (value == null) return false;
		return value.toLowerCase().contains(needleLower);
	}
}
