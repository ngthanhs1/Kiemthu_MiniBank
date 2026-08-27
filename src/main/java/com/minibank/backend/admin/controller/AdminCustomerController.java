package com.minibank.backend.admin.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
import com.minibank.backend.admin.dto.CashAdjustRequest;
import com.minibank.backend.admin.dto.UserSummary;
import com.minibank.backend.transaction.entity.Transaction;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/customers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCustomerController {
	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;
	private final AccountBalanceLedgerRepository ledgerRepository;

	public AdminCustomerController(
		UserRepository userRepository,
		AccountRepository accountRepository,
		TransactionRepository transactionRepository,
		AccountBalanceLedgerRepository ledgerRepository
	) {
		this.userRepository = userRepository;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
		this.ledgerRepository = ledgerRepository;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<UserSummary> list(@RequestParam(value = "q", required = false) String q) {
		String query = q == null ? null : q.trim().toLowerCase();
		return userRepository.findAll().stream()
			.filter(u -> {
				if (query == null || query.isBlank()) return true;
				String phone = u.getPhone() == null ? "" : u.getPhone().toLowerCase();
				String name = u.getFullName() == null ? "" : u.getFullName().toLowerCase();
				return phone.contains(query) || name.contains(query);
			})
			.map(u -> new UserSummary(u.getId(), u.getPhone(), u.getEmail(), u.getFullName(), u.getStatus(), u.getCustomerRank(), u.getDeviceId()))
			.toList();
	}

	@PostMapping("/{userId}/cash-in")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void cashIn(@PathVariable Long userId, @Valid @RequestBody CashAdjustRequest request) {
		adjustBalance(userId, request.amount(), "cash_in");
	}

	@PostMapping("/{userId}/cash-out")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void cashOut(@PathVariable Long userId, @Valid @RequestBody CashAdjustRequest request) {
		adjustBalance(userId, request.amount().negate(), "cash_out");
	}

	@PutMapping("/{userId}")
	@Transactional
	public UserSummary updateCustomer(@PathVariable Long userId, @RequestBody UpdateCustomerRequest request) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (request.fullName() != null) user.setFullName(request.fullName().trim());
		if (request.phone() != null) user.setPhone(request.phone().trim());
		if (request.email() != null) user.setEmail(request.email().trim());
		if (request.citizenId() != null) user.setCitizenId(request.citizenId().trim());
		if (request.address() != null) user.setAddress(request.address().trim());
		if (request.customerRank() != null) user.setCustomerRank(request.customerRank().trim());
		if (request.status() != null) user.setStatus(request.status().trim().toLowerCase());

		User saved = userRepository.save(user);
		return new UserSummary(saved.getId(), saved.getPhone(), saved.getEmail(), saved.getFullName(), saved.getStatus(), saved.getCustomerRank(), saved.getDeviceId());
	}

	@PostMapping("/{userId}/lock")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void lockCustomer(@PathVariable Long userId) {
		setCustomerStatus(userId, "locked");
	}

	@PostMapping("/{userId}/unlock")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Transactional
	public void unlockCustomer(@PathVariable Long userId) {
		setCustomerStatus(userId, "active");
	}

	private void setCustomerStatus(Long userId, String status) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		user.setStatus(status);
		userRepository.save(user);
	}

	private void adjustBalance(Long userId, BigDecimal delta, String type) {
		if (delta == null || delta.compareTo(BigDecimal.ZERO) == 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be non-zero");
		}

		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		Account account = accountRepository.findByUserIdOrderByIdAsc(user.getId()).stream().findFirst()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "User has no account"));

		BigDecimal before = account.getAvailableBalance();
		BigDecimal after = before.add(delta);
		if (after.compareTo(BigDecimal.ZERO) < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient balance");
		}

		account.setAvailableBalance(after);
		account.setCurrentBalance(account.getCurrentBalance().add(delta));
		accountRepository.save(account);

		Transaction tx = Transaction.builder()
			.transactionCode(type.toUpperCase() + "_" + System.currentTimeMillis())
			.transactionType(type)
			.amount(delta.abs())
			.feeAmount(BigDecimal.ZERO)
			.description("Admin balance adjustment")
			.status("completed")
			.toAccount(delta.signum() > 0 ? account : null)
			.fromAccount(delta.signum() < 0 ? account : null)
			.initiatedByUser(user)
			.completedAt(Instant.now())
			.build();
		transactionRepository.save(tx);

		ledgerRepository.save(AccountBalanceLedger.builder()
			.account(account)
			.transaction(tx)
			.entryType(delta.signum() > 0 ? "credit" : "debit")
			.amount(delta.abs())
			.balanceBefore(before)
			.balanceAfter(after)
			.build());
	}

	public record UpdateCustomerRequest(
		String fullName,
		String phone,
		String email,
		String citizenId,
		String address,
		String customerRank,
		String status
	) {}
}
