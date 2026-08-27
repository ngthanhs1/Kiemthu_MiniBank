package com.minibank.backend.user.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.minibank.backend.account.repository.AccountRepository;
import com.minibank.backend.user.entity.User;
import com.minibank.backend.user.repository.UserRepository;

@Service
public class CustomerRankService {
	private final AccountRepository accountRepository;
	private final UserRepository userRepository;
	private final Map<Long, String> rankCache = new ConcurrentHashMap<>();
	private final Map<Long, Instant> rankCacheTime = new ConcurrentHashMap<>();
	private static final long CACHE_TTL_SECONDS = 300;

	public CustomerRankService(AccountRepository accountRepository, UserRepository userRepository) {
		this.accountRepository = accountRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public String refreshRank(User user) {
		Long userId = user.getId();
		Instant cachedAt = rankCacheTime.get(userId);
		if (cachedAt != null && Instant.now().isBefore(cachedAt.plusSeconds(CACHE_TTL_SECONDS))) {
			String cachedRank = rankCache.get(userId);
			if (cachedRank != null) return cachedRank;
		}
		BigDecimal totalBalance = accountRepository.findByUserIdOrderByIdAsc(user.getId()).stream()
			.map(account -> account.getCurrentBalance() == null ? BigDecimal.ZERO : account.getCurrentBalance())
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		String rank = rankForBalance(totalBalance);
		String scoreLevel = creditLevelForRank(rank);
		if (!rank.equalsIgnoreCase(nullToEmpty(user.getCustomerRank()))
			|| !scoreLevel.equalsIgnoreCase(nullToEmpty(user.getCreditScoreLevel()))) {
			user.setCustomerRank(rank);
			user.setCreditScoreLevel(scoreLevel);
			userRepository.save(user);
		}
		rankCache.put(userId, rank);
		rankCacheTime.put(userId, Instant.now());
		return rank;
	}

	public String rankForBalance(BigDecimal balance) {
		BigDecimal value = balance == null ? BigDecimal.ZERO : balance;
		if (value.compareTo(new BigDecimal("2000000000")) >= 0) return "kim_cuong";
		if (value.compareTo(new BigDecimal("1000000000")) >= 0) return "bach_kim";
		if (value.compareTo(new BigDecimal("200000000")) >= 0) return "vang";
		if (value.compareTo(new BigDecimal("50000000")) >= 0) return "bac";
		return "dong";
	}

	private String creditLevelForRank(String rank) {
		return switch (rank) {
			case "kim_cuong", "bach_kim" -> "A";
			case "vang" -> "B";
			case "bac" -> "C";
			default -> "D";
		};
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}
}
