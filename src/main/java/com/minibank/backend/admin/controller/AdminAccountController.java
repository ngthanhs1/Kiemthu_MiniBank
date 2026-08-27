package com.minibank.backend.admin.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.entity.AccountBalanceLedger;
import com.minibank.backend.account.repository.AccountBalanceLedgerRepository;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.admin.dto.AdminAccountOverview;
import com.minibank.backend.admin.dto.AdminAccountSummary;
import com.minibank.backend.admin.dto.AdminBalanceAdjustmentRequest;
import com.minibank.backend.admin.dto.UpdateAccountLimitRequest;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.entity.User;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAccountController {
	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final AccountBalanceLedgerRepository ledgerRepository;

	public AdminAccountController(
			AccountRepository accountRepository,
			TransactionRepository transactionRepository,
			AccountBalanceLedgerRepository ledgerRepository) {
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.ledgerRepository = ledgerRepository;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<AdminAccountSummary> list(
			@RequestParam(value = "q", required = false) String q,
			@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "type", required = false) String type) {
		String query = normalize(q);
		String statusFilter = normalize(status);
		String typeFilter = normalize(type);

		return accountRepository.findAll().stream()
				.filter(a -> statusFilter == null || statusFilter.equalsIgnoreCase(a.getStatus()))
				.filter(a -> typeFilter == null || typeFilter.equalsIgnoreCase(a.getAccountType()))
				.filter(a -> matchesQuery(a, query))
				.map(this::toSummary)
				.toList();
	}

	@GetMapping("/overview")
	@Transactional(readOnly = true)
	public AdminAccountOverview overview() {
		List<Account> accounts = accountRepository.findAll();
		long total = accounts.size();
		long active = accounts.stream().filter(a -> "active".equalsIgnoreCase(a.getStatus())).count();
		long locked = accounts.stream().filter(a -> "locked".equalsIgnoreCase(a.getStatus())).count();
		BigDecimal totalBalance = accounts.stream()
				.map(a -> a.getCurrentBalance() == null ? BigDecimal.ZERO : a.getCurrentBalance())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return new AdminAccountOverview(total, active, locked, totalBalance);
	}

	@PostMapping("/{accountNumber}/deposit-test")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void depositTest(
			@PathVariable String accountNumber,
			@Valid @RequestBody AdminBalanceAdjustmentRequest request) {
		adjustBalance(accountNumber, request.amount(), "cash_in", request.description());
	}

	@PostMapping("/{accountNumber}/withdraw-test")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void withdrawTest(
			@PathVariable String accountNumber,
			@Valid @RequestBody AdminBalanceAdjustmentRequest request) {
		adjustBalance(accountNumber, request.amount().negate(), "cash_out", request.description());
	}

	@PostMapping("/deposit-test")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void depositTestByQuery(
			@RequestParam("accountNumber") String accountNumber,
			@Valid @RequestBody AdminBalanceAdjustmentRequest request) {
		adjustBalance(accountNumber, request.amount(), "cash_in", request.description());
	}

	@PostMapping("/withdraw-test")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void withdrawTestByQuery(
			@RequestParam("accountNumber") String accountNumber,
			@Valid @RequestBody AdminBalanceAdjustmentRequest request) {
		adjustBalance(accountNumber, request.amount().negate(), "cash_out", request.description());
	}

	@PutMapping("/{id}/limits")
	@Transactional
	public void updateLimits(
			@PathVariable Long id,
			@RequestBody UpdateAccountLimitRequest request) {

		Account account = accountRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Account not found"));

		account.setDailyTransferLimit(
				request.dailyTransferLimit());

		account.setDailyReceiveLimit(
				request.dailyReceiveLimit());

		accountRepository.save(account);
	}

	private void adjustBalance(String accountNumber, BigDecimal delta, String type, String description) {
		if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be non-zero");
		}

		String normalizedAccountNumber = accountNumber == null ? "" : accountNumber.trim();
		if (normalizedAccountNumber.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountNumber is required");
		}

		Account account = accountRepository.findByAccountNumberForUpdate(normalizedAccountNumber)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

		BigDecimal availableBefore = nullToZero(account.getAvailableBalance());
		BigDecimal currentBefore = nullToZero(account.getCurrentBalance());
		BigDecimal availableAfter = availableBefore.add(delta);
		BigDecimal currentAfter = currentBefore.add(delta);

		if (availableAfter.compareTo(BigDecimal.ZERO) < 0 || currentAfter.compareTo(BigDecimal.ZERO) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
		}

		account.setAvailableBalance(availableAfter);
		account.setCurrentBalance(currentAfter);
		accountRepository.save(account);

		Transaction tx = Transaction.builder()
				.transactionCode(type.toUpperCase() + "_" + System.currentTimeMillis())
				.transactionType(type)
				.amount(delta.abs())
				.feeAmount(BigDecimal.ZERO)
				.description(description == null || description.isBlank() ? "Admin balance adjustment" : description.trim())
				.status("completed")
				.toAccount(delta.signum() > 0 ? account : null)
				.fromAccount(delta.signum() < 0 ? account : null)
				.initiatedByUser(account.getUser())
				.completedAt(Instant.now())
				.build();
		transactionRepository.save(tx);

		ledgerRepository.save(AccountBalanceLedger.builder()
				.account(account)
				.transaction(tx)
				.entryType(delta.signum() > 0 ? "credit" : "debit")
				.amount(delta.abs())
				.balanceBefore(availableBefore)
				.balanceAfter(availableAfter)
				.build());
	}

	private static BigDecimal nullToZero(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	private AdminAccountSummary toSummary(Account account) {
		User user = account.getUser();
		return new AdminAccountSummary(
				account.getId(),
				account.getAccountNumber(),
				account.getAccountName(),
				account.getAccountType(),
				account.getCurrency(),
				account.getAvailableBalance(),
				account.getCurrentBalance(),
				account.getDailyTransferLimit(),
				account.getDailyReceiveLimit(),
				account.getStatus(),
				account.getOpenedAt(),
				user == null ? null : user.getId(),
				user == null ? null : user.getFullName(),
				user == null ? null : user.getPhone());
	}

	private boolean matchesQuery(Account account, String query) {
		if (query == null) {
			return true;
		}
		String accountNumber = safeLower(account.getAccountNumber());
		String accountName = safeLower(account.getAccountName());
		User user = account.getUser();
		String ownerName = user == null ? "" : safeLower(user.getFullName());
		String ownerPhone = user == null ? "" : safeLower(user.getPhone());
		return accountNumber.contains(query)
				|| accountName.contains(query)
				|| ownerName.contains(query)
				|| ownerPhone.contains(query);
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim().toLowerCase();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static String safeLower(String value) {
		return value == null ? "" : value.toLowerCase();
	}
}
