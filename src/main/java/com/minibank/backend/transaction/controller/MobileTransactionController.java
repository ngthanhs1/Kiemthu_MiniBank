package com.minibank.backend.transaction.controller;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.transaction.dto.MobileTransactionSummary;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.entity.TransactionCategory;
import com.minibank.backend.transaction.repository.TransactionCategoryRepository;
import com.minibank.backend.transaction.repository.TransactionRepository;

@RestController
@RequestMapping("/api/mobile/transactions")
public class MobileTransactionController {
	private final TransactionRepository transactionRepository;
	private final TransactionCategoryRepository transactionCategoryRepository;

	public MobileTransactionController(
		TransactionRepository transactionRepository,
		TransactionCategoryRepository transactionCategoryRepository
	) {
		this.transactionRepository = transactionRepository;
		this.transactionCategoryRepository = transactionCategoryRepository;
	}

	@GetMapping("/recent")
	@Transactional(readOnly = true)
	public List<MobileTransactionSummary> recent(@RequestParam(value = "limit", required = false) Integer limit) {
		long userId = CurrentJwt.requireUserId();
		int finalLimit = limit == null ? 5 : Math.min(Math.max(limit, 1), 50);
		Pageable pageable = PageRequest.of(0, finalLimit);
		List<Transaction> transactions = transactionRepository.findRecentForUser(userId, pageable);
		Map<Long, TransactionCategory> latestCategories = latestCategoryMap(transactions);
		return transactions.stream()
			.filter(this::isMobileVisibleTransaction)
			.map(tx -> toSummary(tx, userId, latestCategories.get(tx.getId())))
			.toList();
	}

	@GetMapping("/history")
	@Transactional(readOnly = true)
	public List<MobileTransactionSummary> history(
		@RequestParam(value = "limit", required = false) Integer limit,
		@RequestParam(value = "page", required = false) Integer page,
		@RequestParam(value = "direction", required = false) String direction,
		@RequestParam(value = "status", required = false) String status,
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "from", required = false) String from,
		@RequestParam(value = "to", required = false) String to
	) {
		long userId = CurrentJwt.requireUserId();
		String dir = normalize(direction);
		String statusFilter = normalize(status);
		String query = q == null ? null : q.trim().toLowerCase(Locale.ROOT);
		java.time.Instant fromInstant = parseInstant(from, "from");
		java.time.Instant toInstant = parseInstant(to, "to");

		Pageable pageable = Pageable.unpaged();
		if (limit != null) {
			int finalLimit = Math.min(Math.max(limit, 1), 50);
			int finalPage = page == null ? 0 : Math.max(page, 0);
			pageable = PageRequest.of(finalPage, finalLimit);
		}
		List<Transaction> transactions = transactionRepository.findForUserWithDateFilter(userId, fromInstant, toInstant, pageable);
		Map<Long, TransactionCategory> latestCategories = latestCategoryMap(transactions);

		java.util.stream.Stream<MobileTransactionSummary> stream = transactions.stream()
			.filter(this::isMobileVisibleTransaction)
			.filter(tx -> matchesDirection(tx, userId, dir))
			.filter(tx -> matchesStatus(tx, statusFilter))
			.filter(tx -> matchesQuery(tx, userId, query))
			.map(tx -> toSummary(tx, userId, latestCategories.get(tx.getId())));

		return stream.toList();
	}

	@GetMapping("/pending")
	@Transactional(readOnly = true)
	public List<MobileTransactionSummary> pending(
		@RequestParam(value = "limit", required = false) Integer limit
	) {
		long userId = CurrentJwt.requireUserId();
		int finalLimit = limit == null ? 20 : Math.min(Math.max(limit, 1), 50);
		Pageable pageable = PageRequest.of(0, finalLimit);
		List<String> statuses = List.of("pending_review", "pending_manager");
		List<Transaction> transactions = transactionRepository.findPendingForUser(userId, statuses, pageable);
		Map<Long, TransactionCategory> latestCategories = latestCategoryMap(transactions);
		return transactions.stream()
			.filter(this::isMobileVisibleTransaction)
			.map(tx -> toSummary(tx, userId, latestCategories.get(tx.getId())))
			.toList();
	}

	private boolean isMobileVisibleTransaction(Transaction tx) {
		String type = tx.getTransactionType();
		if (type == null) {
			return true;
		}
		return !type.trim().toLowerCase(Locale.ROOT).startsWith("saving_");
	}

	private static MobileTransactionSummary toSummary(
		Transaction tx,
		long userId,
		TransactionCategory category
	) {
		Account from = tx.getFromAccount();
		Account to = tx.getToAccount();
		boolean incoming = to != null && to.getUser() != null && userId == to.getUser().getId();
		Account counterparty = incoming ? from : to;
		String categoryCode = category == null ? null : category.getCategoryCode();
		String categorySource = category == null ? null : category.getSource();
		java.math.BigDecimal confidence = category == null ? null : category.getConfidence();
		return new MobileTransactionSummary(
			tx.getId(),
			incoming ? "in" : "out",
			tx.getAmount(),
			tx.getDescription(),
			counterparty == null ? null : counterparty.getAccountNumber(),
			counterparty == null ? null : counterparty.getAccountName(),
			tx.getTransactionType(),
			tx.getStatus(),
			tx.getCreatedAt(),
			categoryCode,
			categorySource,
			confidence
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

	private static boolean matchesDirection(Transaction tx, long userId, String direction) {
		if (direction == null || direction.isBlank() || direction.equals("all")) {
			return true;
		}
		boolean incoming = isIncoming(tx, userId);
		return direction.equals("in") ? incoming : !incoming;
	}

	private static boolean matchesStatus(Transaction tx, String status) {
		if (status == null || status.isBlank() || "all".equals(status)) {
			return true;
		}
		String normalized = status.toLowerCase(Locale.ROOT);
		for (String token : normalized.split(",")) {
			String s = token.trim();
			if (!s.isEmpty() && s.equalsIgnoreCase(tx.getStatus())) {
				return true;
			}
		}
		return false;
	}

	private static boolean matchesQuery(Transaction tx, long userId, String query) {
		if (query == null || query.isBlank()) {
			return true;
		}
		String transactionCode = tx.getTransactionCode() == null ? "" : tx.getTransactionCode().toLowerCase(Locale.ROOT);
		if (transactionCode.contains(query)) {
			return true;
		}
		Account counterparty = isIncoming(tx, userId) ? tx.getFromAccount() : tx.getToAccount();
		return containsIgnoreCase(tx.getDescription(), query)
			|| containsIgnoreCase(tx.getTransactionType(), query)
			|| containsIgnoreCase(counterparty == null ? null : counterparty.getAccountNumber(), query)
			|| containsIgnoreCase(counterparty == null ? null : counterparty.getAccountName(), query);
	}

	private static boolean containsIgnoreCase(String value, String query) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(query);
	}

	private static boolean isIncoming(Transaction tx, long userId) {
		Account to = tx.getToAccount();
		return to != null && to.getUser() != null && Objects.equals(to.getUser().getId(), userId);
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim().toLowerCase(Locale.ROOT);
		return trimmed.isBlank() ? null : trimmed;
	}

	private static java.time.Instant parseInstant(String value, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return java.time.Instant.parse(value.trim());
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " must be ISO-8601 instant");
		}
	}

}
