package com.minibank.backend.account.service;

import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.minibank.backend.account.repository.AccountRepository;

@Service
public class AccountNumberService {
	private static final int ACCOUNT_NUMBER_LENGTH = 13;
	private static final SecureRandom RNG = new SecureRandom();
	private final AccountRepository accountRepository;

	public AccountNumberService(AccountRepository accountRepository) {
		this.accountRepository = accountRepository;
	}

	public String generateUniqueAccountNumber() {
		for (int i = 0; i < 5000; i++) {
			String candidate = randomDigits(ACCOUNT_NUMBER_LENGTH);
			if (!accountRepository.existsByAccountNumber(candidate)) {
				return candidate;
			}
		}
		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to generate account number");
	}

	public String generateUniqueAccountNumberWithDesired(String desiredDigits) {
		String desired = desiredDigits == null ? null : desiredDigits.trim();
		if (desired == null || desired.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "desired is required");
		}
		if (!desired.matches("^[0-9]{6,8}$")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "desired must be 6-8 digits");
		}

		int remaining = ACCOUNT_NUMBER_LENGTH - desired.length();
		if (remaining < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "desired is too long");
		}

		for (int i = 0; i < 5000; i++) {
			int prefixLen = remaining == 0 ? 0 : RNG.nextInt(remaining + 1);
			int suffixLen = remaining - prefixLen;
			String candidate = randomDigits(prefixLen) + desired + randomDigits(suffixLen);
			if (!accountRepository.existsByAccountNumber(candidate)) {
				return candidate;
			}
		}

		throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to generate account number");
	}

	public List<String> suggestAccountNumbers(String desiredDigits, int limit) {
		String desired = desiredDigits == null ? null : desiredDigits.trim();
		if (desired == null || desired.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "desired is required");
		}
		if (!desired.matches("^[0-9]{6,8}$")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "desired must be 6-8 digits");
		}
		if (limit <= 0 || limit > 50) {
			limit = 10;
		}

		Set<String> out = new LinkedHashSet<>();
		int desiredLen = desired.length();
		int remaining = ACCOUNT_NUMBER_LENGTH - desiredLen;

		// Try common placements first: start, end, middle-ish.
		int[] preferredPrefixLens = new int[] { 0, remaining, remaining / 2 };
		for (int prefixLen : preferredPrefixLens) {
			for (int j = 0; j < 50 && out.size() < limit; j++) {
				int safePrefixLen = Math.max(0, Math.min(prefixLen, remaining));
				int suffixLen = remaining - safePrefixLen;
				String candidate = randomDigits(safePrefixLen) + desired + randomDigits(suffixLen);
				if (!accountRepository.existsByAccountNumber(candidate)) {
					out.add(candidate);
				}
			}
		}

		// Then fill with random placements.
		for (int i = 0; i < 5000 && out.size() < limit; i++) {
			int prefixLen = remaining == 0 ? 0 : RNG.nextInt(remaining + 1);
			int suffixLen = remaining - prefixLen;
			String candidate = randomDigits(prefixLen) + desired + randomDigits(suffixLen);
			if (!accountRepository.existsByAccountNumber(candidate)) {
				out.add(candidate);
			}
		}

		return List.copyOf(out);
	}

	private static String randomDigits(int length) {
		if (length <= 0) return "";
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append((char) ('0' + RNG.nextInt(10)));
		}
		return sb.toString();
	}
}
