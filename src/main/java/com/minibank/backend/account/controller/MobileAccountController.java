package com.minibank.backend.account.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.dto.AccountQrResponse;
import com.minibank.backend.account.dto.AccountResolveResponse;
import com.minibank.backend.account.dto.AccountSuggestionsResponse;
import com.minibank.backend.account.dto.AccountSummaryResponse;
import com.minibank.backend.account.dto.CreateAccountRequest;
import com.minibank.backend.account.entity.Account;
import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.account.service.AccountNumberService;
import com.minibank.backend.account.service.AccountQrService;
import com.minibank.backend.common.security.CurrentJwt;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;
import com.minibank.backend.user.service.CustomerRankService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/accounts")
public class MobileAccountController {
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final AccountNumberService accountNumberService;
	private final AccountQrService accountQrService;
	private final CustomerRankService customerRankService;

	public MobileAccountController(
		AccountRepository accountRepository,
		UserRepository userRepository,
		AccountNumberService accountNumberService,
		AccountQrService accountQrService,
		CustomerRankService customerRankService
	) {
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
		this.accountNumberService = accountNumberService;
		this.accountQrService = accountQrService;
		this.customerRankService = customerRankService;
	}

	@GetMapping("/me")
	@Transactional(readOnly = true)
	public List<AccountResolveResponse> myAccounts() {
		long userId = CurrentJwt.requireUserId();
		return accountRepository.findByUserIdOrderByIdAsc(userId).stream()
			.map(a -> new AccountResolveResponse(a.getId(), a.getAccountNumber(), a.getAccountName()))
			.toList();
	}

	@GetMapping("/summary")
	@Transactional
	public AccountSummaryResponse summary() {
		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

		Account account = accountRepository.findByUserIdOrderByIdAsc(userId).stream()
			.findFirst()
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "No account is assigned"));

		String customerRank = customerRankService.refreshRank(user);
		return new AccountSummaryResponse(
			account.getAccountNumber(),
			account.getAccountName(),
			account.getAvailableBalance(),
			account.getCurrentBalance(),
			account.getStatus(),
			customerRank,
			account.getDailyTransferLimit(),
			account.getDailyReceiveLimit()
		);
	}

	@GetMapping("/resolve")
	@Transactional(readOnly = true)
	public AccountResolveResponse resolve(@RequestParam("accountNumber") String accountNumber) {
		String accNo = accountNumber == null ? null : accountNumber.trim();
		if (accNo == null || accNo.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "accountNumber is required");
		}

		Account acc = accountRepository.findByAccountNumber(accNo)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
		return new AccountResolveResponse(acc.getId(), acc.getAccountNumber(), acc.getAccountName());
	}

	@GetMapping("/suggestions")
	public AccountSuggestionsResponse suggestions(
		@RequestParam(name = "desired", required = false) String desired,
		@RequestParam(name = "limit", required = false) Integer limit
	) {
		if (desired == null || desired.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "desired is required");
		}
		int finalLimit = limit == null ? 10 : limit;
		return new AccountSuggestionsResponse(desired, accountNumberService.suggestAccountNumbers(desired, finalLimit));
	}

	@PostMapping("/me/create")
	@ResponseStatus(HttpStatus.CREATED)
	@Transactional
	public AccountResolveResponse createMyAccount(@Valid @RequestBody CreateAccountRequest request) {
		long userId = CurrentJwt.requireUserId();
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
		if (!"active".equalsIgnoreCase(user.getStatus())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not approved yet");
		}
		if (!accountRepository.findByUserIdOrderByIdAsc(userId).isEmpty()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists");
		}

		String accountNumber = request.accountNumber().trim();
		if (accountRepository.existsByAccountNumber(accountNumber)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "accountNumber already exists");
		}

		String accountName = (user.getFullName() == null || user.getFullName().isBlank()) ? user.getPhone() : user.getFullName();
		Account account = Account.builder()
			.user(user)
			.accountNumber(accountNumber)
			.accountName(accountName)
			.accountType("payment")
			.currency("VND")
			.availableBalance(BigDecimal.ZERO)
			.currentBalance(BigDecimal.ZERO)
			.dailyTransferLimit(new BigDecimal("50000000"))
			.dailyReceiveLimit(new BigDecimal("50000000"))
			.status("active")
			.openedAt(Instant.now())
			.build();
		accountRepository.save(account);

		return new AccountResolveResponse(account.getId(), account.getAccountNumber(), account.getAccountName());
	}

	@GetMapping("/me/qr")
	@Transactional
	public AccountQrResponse myQr(@RequestParam(value = "accountNumber", required = false) String accountNumber) {
		long userId = CurrentJwt.requireUserId();
		Account account;
		if (accountNumber != null && !accountNumber.isBlank()) {
			account = accountRepository.findByAccountNumber(accountNumber.trim())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
			if (account.getUser() == null || account.getUser().getId() == null || account.getUser().getId() != userId) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account does not belong to user");
			}
		} else {
			account = accountRepository.findByUserIdOrderByIdAsc(userId).stream()
				.findFirst()
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "No account is assigned"));
		}

		String payload = accountQrService.getOrCreateActivePayload(account);
		return new AccountQrResponse(account.getAccountNumber(), account.getAccountName(), payload);
	}
}
