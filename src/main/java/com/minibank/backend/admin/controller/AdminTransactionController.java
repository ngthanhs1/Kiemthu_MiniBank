package com.minibank.backend.admin.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.admin.dto.AdminPendingLargeTransactionResponse;
import com.minibank.backend.admin.dto.AdminTransactionClassificationResponse;
import com.minibank.backend.admin.dto.AdminTransactionOverview;
import com.minibank.backend.admin.dto.AdminTransactionSummary;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.entity.AccountBalanceLedger;
import com.minibank.backend.account.repository.AccountBalanceLedgerRepository;
import com.minibank.backend.transaction.entity.TransactionCategory;
import com.minibank.backend.transaction.repository.TransactionCategoryRepository;
import com.minibank.backend.transaction.repository.TransactionRepository;

@RestController
@RequestMapping("/api/admin/transactions")
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransactionController {
	private final TransactionRepository transactionRepository;
	private final TransactionCategoryRepository transactionCategoryRepository;
	private final AccountBalanceLedgerRepository ledgerRepository;
	private final BigDecimal pendingThreshold;
	private final BigDecimal highRiskThreshold;

	public AdminTransactionController(
		TransactionRepository transactionRepository,
		TransactionCategoryRepository transactionCategoryRepository,
		AccountBalanceLedgerRepository ledgerRepository,
		@Value("${app.transactions.pending-approval-threshold:100000000}") BigDecimal pendingThreshold,
		@Value("${app.transactions.high-risk-threshold:200000000}") BigDecimal highRiskThreshold
	) {
		this.transactionRepository = transactionRepository;
		this.transactionCategoryRepository = transactionCategoryRepository;
		this.ledgerRepository = ledgerRepository;
		this.pendingThreshold = pendingThreshold;
		this.highRiskThreshold = highRiskThreshold;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<AdminTransactionSummary> list(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "minAmount", required = false) BigDecimal minAmount,
		@RequestParam(value = "maxAmount", required = false) BigDecimal maxAmount,
		@RequestParam(value = "from", required = false) String from,
		@RequestParam(value = "to", required = false) String to
	) {
		String query = normalize(q);
		String statusFilter = normalize(status);
		Instant fromInstant = parseInstant(from, "from");
		Instant toInstant = parseInstant(to, "to");

		return transactionRepository.findAllWithAccounts().stream()
			.filter(this::isAdminVisibleTransaction)
			.filter(tx -> statusFilter == null || statusFilter.equalsIgnoreCase(tx.getStatus()))
			.filter(tx -> minAmount == null || tx.getAmount().compareTo(minAmount) >= 0)
			.filter(tx -> maxAmount == null || tx.getAmount().compareTo(maxAmount) <= 0)
			.filter(tx -> fromInstant == null || !tx.getCreatedAt().isBefore(fromInstant))
			.filter(tx -> toInstant == null || !tx.getCreatedAt().isAfter(toInstant))
			.filter(tx -> matchesQuery(tx, query))
			.map(this::toSummary)
			.toList();
	}

	@GetMapping("/overview")
	@Transactional(readOnly = true)
	public AdminTransactionOverview overview() {
		List<Transaction> transactions = transactionRepository.findAll().stream()
			.filter(this::isAdminVisibleTransaction)
			.toList();
		long total = transactions.size();
		long completed = transactions.stream().filter(tx -> "completed".equalsIgnoreCase(tx.getStatus())).count();
		BigDecimal totalAmount = transactions.stream()
			.filter(tx -> "completed".equalsIgnoreCase(tx.getStatus()))
			.map(Transaction::getAmount)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new AdminTransactionOverview(total, completed, totalAmount);
	}

	@PostMapping("/{id}/refund")
	@Transactional
	public AdminTransactionSummary refund(@PathVariable Long id) {
		Transaction original = transactionRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
		if (!"completed".equalsIgnoreCase(original.getStatus())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only completed transactions can be refunded");
		}
		if ("refunded".equalsIgnoreCase(original.getStatus()) || "refund".equalsIgnoreCase(original.getTransactionType())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction already refunded");
		}
		Account from = original.getFromAccount();
		Account to = original.getToAccount();
		if (from == null || to == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cash/system transactions cannot be refunded automatically");
		}
		BigDecimal amount = original.getAmount();
		if (to.getAvailableBalance().compareTo(amount) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Destination account has insufficient balance for refund");
		}

		BigDecimal toBefore = to.getAvailableBalance();
		BigDecimal fromBefore = from.getAvailableBalance();
		to.setAvailableBalance(toBefore.subtract(amount));
		to.setCurrentBalance(to.getCurrentBalance().subtract(amount));
		from.setAvailableBalance(fromBefore.add(amount));
		from.setCurrentBalance(from.getCurrentBalance().add(amount));
		original.setStatus("refunded");
		original.setCompletedAt(Instant.now());

		Transaction refund = Transaction.builder()
			.transactionCode("REFUND_" + original.getId() + "_" + System.currentTimeMillis())
			.fromAccount(to)
			.toAccount(from)
			.transactionType("refund")
			.amount(amount)
			.feeAmount(BigDecimal.ZERO)
			.description("Refund for transaction " + original.getTransactionCode())
			.status("completed")
			.initiatedByUser(original.getInitiatedByUser())
			.completedAt(Instant.now())
			.build();
		transactionRepository.save(original);
		Transaction savedRefund = transactionRepository.save(refund);

		ledgerRepository.save(AccountBalanceLedger.builder().account(to).transaction(savedRefund).entryType("debit").amount(amount).balanceBefore(toBefore).balanceAfter(to.getAvailableBalance()).build());
		ledgerRepository.save(AccountBalanceLedger.builder().account(from).transaction(savedRefund).entryType("credit").amount(amount).balanceBefore(fromBefore).balanceAfter(from.getAvailableBalance()).build());
		return toSummary(savedRefund);
	}

	@GetMapping("/classifications")
	@Transactional(readOnly = true)
	public List<AdminTransactionClassificationResponse> classifications(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "category", required = false) String category
	) {
		String query = normalize(q);
		String categoryFilter = normalize(category);
		List<Transaction> transactions = transactionRepository.findAllWithAccounts().stream()
			.filter(this::isAdminVisibleTransaction)
			.toList();
		Map<Long, TransactionCategory> latestCategories = latestCategoryMap(transactions);

		return transactions.stream()
			.filter(tx -> matchesQuery(tx, query))
			.map(tx -> toClassification(tx, latestCategories.get(tx.getId())))
			.filter(resp -> categoryFilter == null || categoryFilter.equalsIgnoreCase(resp.categoryCode()))
			.sorted(Comparator.comparing(AdminTransactionClassificationResponse::createdAt).reversed())
			.toList();
	}

	@GetMapping("/pending-large")
	@Transactional(readOnly = true)
	public List<AdminPendingLargeTransactionResponse> pendingLarge(
		@RequestParam(value = "minAmount", required = false) BigDecimal minAmount
	) {
		BigDecimal threshold = minAmount == null ? pendingThreshold : minAmount;
		return transactionRepository.findAllWithAccounts().stream()
			.filter(this::isAdminVisibleTransaction)
			.filter(tx -> "pending".equalsIgnoreCase(tx.getStatus()) || "pending_review".equalsIgnoreCase(tx.getStatus()))
			.filter(tx -> tx.getAmount().compareTo(threshold) >= 0)
			.map(tx -> toPendingLarge(tx, threshold))
			.toList();
	}

	private AdminTransactionSummary toSummary(Transaction tx) {
		return new AdminTransactionSummary(
			tx.getId(),
			tx.getTransactionCode(),
			tx.getFromAccount() == null ? null : tx.getFromAccount().getAccountNumber(),
			tx.getFromAccount() == null ? null : tx.getFromAccount().getAccountName(),
			tx.getToAccount() == null ? null : tx.getToAccount().getAccountNumber(),
			tx.getToAccount() == null ? null : tx.getToAccount().getAccountName(),
			tx.getAmount(),
			tx.getFeeAmount(),
			tx.getTransactionType(),
			tx.getStatus(),
			tx.getCreatedAt()
		);
	}

	private AdminTransactionClassificationResponse toClassification(Transaction tx, TransactionCategory category) {
		String categoryCode = category == null ? "khac" : category.getCategoryCode();
		String categoryName = categoryName(categoryCode);
		String source = category == null ? "unknown" : category.getSource();
		String verificationStatus = isVerifiedSource(source) ? "verified" : "pending";

		String accountNumber = tx.getFromAccount() == null ? null : tx.getFromAccount().getAccountNumber();
		String accountName = tx.getFromAccount() == null ? null : tx.getFromAccount().getAccountName();
		if (accountNumber == null && tx.getToAccount() != null) {
			accountNumber = tx.getToAccount().getAccountNumber();
			accountName = tx.getToAccount().getAccountName();
		}

		return new AdminTransactionClassificationResponse(
			tx.getId(),
			tx.getTransactionCode(),
			tx.getCreatedAt(),
			accountName,
			accountNumber,
			tx.getDescription(),
			tx.getAmount(),
			categoryCode,
			categoryName,
			category == null ? BigDecimal.valueOf(0.3) : safeConfidence(category.getConfidence()),
			source,
			verificationStatus
		);
	}

	private Map<Long, TransactionCategory> latestCategoryMap(List<Transaction> transactions) {
		List<Long> ids = transactions.stream().map(Transaction::getId).toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		return transactionCategoryRepository.findByTransactionIdIn(ids).stream()
			.filter(cat -> cat.getTransaction() != null && cat.getTransaction().getId() != null)
			.sorted(Comparator.comparing(TransactionCategory::getTaggedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
			.collect(Collectors.toMap(
				cat -> cat.getTransaction().getId(),
				Function.identity(),
				(first, second) -> first
			));
	}

	private String categoryName(String code) {
		String normalized = code == null ? "" : code.toLowerCase();
		return switch (normalized) {
			case "an_uong" -> "An uong";
			case "mua_sam" -> "Mua sam";
			case "di_chuyen" -> "Di chuyen";
			case "tien_ich" -> "Tien ich";
			case "giai_tri" -> "Giai tri";
			case "giao_duc" -> "Giao duc";
			case "y_te" -> "Y te";
			case "luong" -> "Luong";
			case "chuyen_khoan" -> "Chuyen khoan";
			case "hoan_tien" -> "Hoan tien";
			case "thu_nhap_khac" -> "Thu nhap khac";
			default -> "Khac";
		};
	}

	private boolean isVerifiedSource(String source) {
		if (source == null) {
			return false;
		}
		String normalized = source.trim().toLowerCase();
		return normalized.equals("manual") || normalized.equals("mobile") || normalized.equals("user");
	}

	private AdminPendingLargeTransactionResponse toPendingLarge(Transaction tx, BigDecimal threshold) {
		String riskLevel;
		if (tx.getAmount().compareTo(highRiskThreshold) >= 0) {
			riskLevel = "high";
		} else if (tx.getAmount().compareTo(threshold) >= 0) {
			riskLevel = "medium";
		} else {
			riskLevel = "low";
		}
		return new AdminPendingLargeTransactionResponse(
			tx.getId(),
			tx.getTransactionCode(),
			tx.getFromAccount() == null ? null : tx.getFromAccount().getAccountNumber(),
			tx.getFromAccount() == null ? null : tx.getFromAccount().getAccountName(),
			tx.getToAccount() == null ? null : tx.getToAccount().getAccountNumber(),
			tx.getToAccount() == null ? null : tx.getToAccount().getAccountName(),
			tx.getAmount(),
			tx.getStatus(),
			riskLevel,
			tx.getCreatedAt()
		);
	}

	private boolean matchesQuery(Transaction tx, String query) {
		if (query == null) {
			return true;
		}
		String code = safeLower(tx.getTransactionCode());
		String fromAcc = tx.getFromAccount() == null ? "" : safeLower(tx.getFromAccount().getAccountNumber());
		String toAcc = tx.getToAccount() == null ? "" : safeLower(tx.getToAccount().getAccountNumber());
		String fromName = tx.getFromAccount() == null ? "" : safeLower(tx.getFromAccount().getAccountName());
		String toName = tx.getToAccount() == null ? "" : safeLower(tx.getToAccount().getAccountName());
		String description = safeLower(tx.getDescription());
		return code.contains(query)
			|| fromAcc.contains(query)
			|| toAcc.contains(query)
			|| fromName.contains(query)
			|| toName.contains(query)
			|| description.contains(query);
	}

	private boolean isAdminVisibleTransaction(Transaction tx) {
		String type = tx.getTransactionType();
		if (type == null) {
			return true;
		}
		String normalized = type.trim().toLowerCase();
		return !normalized.startsWith("saving_");
	}

	private BigDecimal safeConfidence(BigDecimal confidence) {
		return confidence == null ? BigDecimal.valueOf(0.3) : confidence;
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim().toLowerCase();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static Instant parseInstant(String value, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(value.trim());
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be ISO-8601 instant");
		}
	}

	private static String safeLower(String value) {
		return value == null ? "" : value.toLowerCase();
	}
}
