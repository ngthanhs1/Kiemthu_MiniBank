package com.minibank.backend.admin.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.AccountBalanceLedger;
import com.minibank.backend.account.repository.AccountBalanceLedgerRepository;
import com.minibank.backend.admin.dto.AdminLedgerEntryResponse;

@RestController
@RequestMapping("/api/admin/ledger")
@PreAuthorize("hasRole('ADMIN')")
public class AdminLedgerController {
	private final AccountBalanceLedgerRepository ledgerRepository;

	public AdminLedgerController(AccountBalanceLedgerRepository ledgerRepository) {
		this.ledgerRepository = ledgerRepository;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<AdminLedgerEntryResponse> list(
		@RequestParam(value = "q", required = false) String q,
		@RequestParam(value = "entryType", required = false) String entryType,
		@RequestParam(value = "from", required = false) String from,
		@RequestParam(value = "to", required = false) String to
	) {
		String query = normalize(q);
		String typeFilter = normalize(entryType);
		Instant fromInstant = parseInstant(from, "from");
		Instant toInstant = parseInstant(to, "to");

		return ledgerRepository.findAllWithAccountAndTransaction().stream()
			.filter(entry -> typeFilter == null || typeFilter.equalsIgnoreCase(entry.getEntryType()))
			.filter(entry -> fromInstant == null || !entry.getCreatedAt().isBefore(fromInstant))
			.filter(entry -> toInstant == null || !entry.getCreatedAt().isAfter(toInstant))
			.filter(entry -> matchesQuery(entry, query))
			.map(this::toResponse)
			.toList();
	}

	private AdminLedgerEntryResponse toResponse(AccountBalanceLedger entry) {
		return new AdminLedgerEntryResponse(
			entry.getId(),
			entry.getAccount().getAccountNumber(),
			entry.getAccount().getAccountName(),
			entry.getEntryType(),
			entry.getAmount(),
			entry.getBalanceBefore(),
			entry.getBalanceAfter(),
			entry.getTransaction() == null ? null : entry.getTransaction().getTransactionCode(),
			entry.getCreatedAt()
		);
	}

	private boolean matchesQuery(AccountBalanceLedger entry, String query) {
		if (query == null) {
			return true;
		}
		String accountNumber = safeLower(entry.getAccount().getAccountNumber());
		String accountName = safeLower(entry.getAccount().getAccountName());
		String transactionCode = entry.getTransaction() == null ? "" : safeLower(entry.getTransaction().getTransactionCode());
		return accountNumber.contains(query) || accountName.contains(query) || transactionCode.contains(query);
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
