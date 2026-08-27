package com.minibank.backend.admin.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.admin.dto.SystemOverviewResponse;
import com.minibank.backend.transaction.repository.TransactionRepository;
import com.minibank.backend.user.repository.UserRepository;

@RestController
@RequestMapping("/api/admin/system")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'Xem dashboard')")
public class AdminSystemController {
	private final UserRepository userRepository;
	private final AccountRepository accountRepository;
	private final TransactionRepository transactionRepository;

	public AdminSystemController(UserRepository userRepository, AccountRepository accountRepository, TransactionRepository transactionRepository) {
		this.userRepository = userRepository;
		this.accountRepository = accountRepository;
		this.transactionRepository = transactionRepository;
	}

	@GetMapping("/overview")
	@Transactional(readOnly = true)
	public SystemOverviewResponse overview() {
		long totalUsers = userRepository.count();
		long pendingUsers = userRepository.findAll().stream().filter(u -> "pending".equalsIgnoreCase(u.getStatus())).count();
		long totalAccounts = accountRepository.count();
		long totalTx = transactionRepository.count();
		long pendingTx = transactionRepository.countByStatus("pending");
		return new SystemOverviewResponse(totalUsers, pendingUsers, totalAccounts, totalTx, pendingTx);
	}
}
